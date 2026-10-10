import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * End-to-end smoke test for MetroServer, the self-hosted Listen Together backend.
 *
 * <p>Deliberately does NOT reuse any MetroServer code: the protobuf reader/writer,
 * the WebSocket client and the gzip envelope handling below are a second, independent
 * implementation of the same wire contract the Android client speaks. That way a pass
 * means "an outside client can really talk to this server", not "the server agrees with
 * itself".
 *
 * <pre>
 *   java metroserver/MetroServer.java 8080     # terminal 1
 *   java metroserver/SmokeTest.java 8080       # terminal 2
 * </pre>
 *
 * Exits with status 1 if any expectation fails.
 */
public final class SmokeTest {

    static final int COMPRESSION_THRESHOLD = 100;
    static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    static int passed;
    static int failed;

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8080;
        for (String arg : args) {
            if (arg.startsWith("--host=")) host = arg.substring(7);
            else if (arg.startsWith("--port=")) port = Integer.parseInt(arg.substring(7));
            else port = Integer.parseInt(arg);
        }

        System.out.println("MetroServer smoke test -> ws://" + host + ":" + port + "/ws");

        // ── plain HTTP status endpoint ──
        section("HTTP status endpoint");
        String health = httpGet(host, port);
        check("status endpoint answers JSON", health != null && health.contains("\"status\":\"ok\""),
                String.valueOf(health));

        // ── create room ──
        section("create room");
        Client neo = new Client("host", host, port);
        neo.send(T.CREATE_ROOM, new W().str(1, "Neo").done());
        Msg created = neo.await(T.ROOM_CREATED, 3000);
        String code = created.str(1);
        String neoId = created.str(2);
        String neoToken = created.str(3);
        check("room_created carries a 6-char code", code.length() == 6 && onlyRoomAlphabet(code), code);
        check("room_created carries the host user id", !neoId.isEmpty(), neoId);
        check("room_created carries a session token", !neoToken.isEmpty(), neoToken);

        // ── joining a room that does not exist ──
        section("join errors");
        Client ghost = new Client("ghost", host, port);
        ghost.send(T.JOIN_ROOM, new W().str(1, "ZZZZZZ").str(2, "Nobody").done());
        Msg rejected = ghost.await(T.JOIN_REJECTED, 3000);
        check("unknown room is rejected", rejected.str(1).contains("not found"), rejected.str(1));
        // An empty join payload must not crash the server either.
        ghost.send(T.JOIN_ROOM, new byte[0]);
        Msg rejectedEmpty = ghost.await(T.JOIN_REJECTED, 3000);
        check("empty join payload is rejected cleanly", !rejectedEmpty.str(1).isEmpty(), rejectedEmpty.str(1));
        ghost.close();

        // ── guest joins and the host approves ──
        section("join request + approval");
        Client trinity = new Client("guest", host, port);
        trinity.send(T.JOIN_ROOM, new W().str(1, code).str(2, "Trinity").done());
        Msg request = neo.await(T.JOIN_REQUEST, 3000);
        String trinityId = request.str(1);
        check("host receives join_request", request.str(2).equals("Trinity"), request.str(2));
        check("join_request carries the guest id", !trinityId.isEmpty(), trinityId);

        neo.send(T.APPROVE_JOIN, new W().str(1, trinityId).done());
        Msg approved = trinity.await(T.JOIN_APPROVED, 3000);
        String trinityToken = approved.str(3);
        check("guest receives join_approved for the same room", approved.str(1).equals(code), approved.str(1));
        check("join_approved carries the guest id", approved.str(2).equals(trinityId), approved.str(2));
        check("join_approved carries a session token", !trinityToken.isEmpty(), trinityToken);
        Msg approvedState = approved.msg(4);
        check("join_approved embeds a room state", approvedState != null, "missing state");
        if (approvedState != null) {
            check("state has the room code", approvedState.str(1).equals(code), approvedState.str(1));
            check("state points at the host", approvedState.str(2).equals(neoId), approvedState.str(2));
            check("state lists both members", approvedState.all(3).size() == 2, "" + approvedState.all(3).size());
            check("state volume is present (proto3 scalar)",
                    approvedState.raw.has(8), "volume field absent");
            check("state starts paused", !approvedState.bool(5), "is_playing true");
            check("state has no track yet", approvedState.msg(4) == null, "unexpected track");
        }
        Msg joined = neo.await(T.USER_JOINED, 3000);
        check("host is told the guest joined", joined.str(1).equals(trinityId) && joined.str(2).equals("Trinity"),
                joined.str(1) + "/" + joined.str(2));

        // ── clock sync ──
        section("ping / pong clock sync");
        trinity.send(T.PING, new W().i64(1, 1234).i64(2, 7).done());
        Msg pong = trinity.await(T.PONG, 3000);
        check("pong echoes client_time", pong.longVal(1) == 1234, "" + pong.longVal(1));
        check("pong echoes sequence", pong.longVal(4) == 7, "" + pong.longVal(4));
        long receive = pong.longVal(2);
        long send = pong.longVal(3);
        check("pong timestamps are sane", receive > 0 && send >= receive, receive + "/" + send);

        // ── initial sync ──
        section("request_sync");
        trinity.send(T.REQUEST_SYNC, new byte[0]);
        Msg state = trinity.await(T.SYNC_STATE, 3000);
        check("sync_state reports paused", !state.bool(2), "is_playing true");
        check("sync_state reports volume", Math.abs(state.f32(6) - 1f) < 0.001f, "" + state.f32(6));
        check("sync_state starts at revision 0", state.longVal(7) == 0, "" + state.longVal(7));
        check("sync_state has no queue yet", state.all(5).isEmpty(), "" + state.all(5).size());

        // ── host-only guard ──
        section("host-only guard");
        trinity.send(T.PLAYBACK_ACTION, new W().str(1, "play").alwaysI64(3, 1000).done());
        Msg notHost = trinity.await(T.ERROR, 3000);
        check("guests cannot drive playback", notHost.str(1).equals("not_host"), notHost.str(1) + "/" + notHost.str(2));
        trinity.send(T.KICK_USER, new W().str(1, neoId).done());
        Msg kickDenied = trinity.await(T.ERROR, 3000);
        check("guests cannot kick", kickDenied.str(1).equals("not_host"), kickDenied.str(1));

        // ── track change with a queue ──
        section("change_track broadcast");
        byte[] t1 = track("t1", "Harleys In Hawaii", "Katy Perry", 182_000);
        byte[] t2 = track("t2", "Chained To The Rhythm", "Katy Perry", 228_000);
        byte[] t3 = track("t3", "Roulette", "Katy Perry", 209_000);
        W change = new W().str(1, "change_track").str(2, "t1").msg(4, t1).msg(6, t1).msg(6, t2).msg(6, t3);
        neo.send(T.PLAYBACK_ACTION, change.done());
        Msg changeEcho = trinity.await(T.SYNC_PLAYBACK, 3000);
        check("guest receives sync_playback", changeEcho.str(1).equals("change_track"), changeEcho.str(1));
        Msg echoTrack = changeEcho.msg(4);
        check("sync_playback carries the track",
                echoTrack != null && echoTrack.str(1).equals("t1") && echoTrack.str(2).equals("Harleys In Hawaii"),
                echoTrack == null ? "no track" : echoTrack.str(1));
        check("sync_playback carries the queue", changeEcho.all(6).size() == 3, "" + changeEcho.all(6).size());
        check("sync_playback is stamped with revision 1", changeEcho.longVal(10) == 1, "" + changeEcho.longVal(10));
        check("changing track does not include a position", !changeEcho.raw.has(3), "position present");
        check("host does not receive its own action", neo.quiet(400) == null, "echoed back to host");
        Msg trackState = neo.state(3000);
        check("room state now reports the track", trackState.msg(1) != null, "no current_track");
        check("room state reset to paused at position 0",
                !trackState.bool(2) && trackState.longVal(3) == 0,
                "playing=" + trackState.bool(2) + " pos=" + trackState.longVal(3));

        // ── play with a position ──
        section("play / pause broadcast");
        neo.send(T.PLAYBACK_ACTION, new W().str(1, "play").str(2, "t1").alwaysI64(3, 5000).done());
        Msg playEcho = trinity.await(T.SYNC_PLAYBACK, 3000);
        check("play relays a position", playEcho.str(1).equals("play") && playEcho.longVal(3) == 5000,
                playEcho.str(1) + " pos=" + playEcho.longVal(3));
        check("play is stamped with revision 2", playEcho.longVal(10) == 2, "" + playEcho.longVal(10));

        trinity.send(T.REQUEST_SYNC, new byte[0]);
        Msg playing = trinity.await(T.SYNC_STATE, 3000);
        check("sync_state reports playing", playing.bool(2), "is_playing false");
        check("sync_state advances the position", playing.longVal(3) >= 5000, "" + playing.longVal(3));
        check("sync_state keeps revision continuity", playing.longVal(7) == 2, "" + playing.longVal(7));
        check("sync_state carries the queue", playing.all(5).size() == 3, "" + playing.all(5).size());

        // ── buffering handshake ──
        section("buffer handshake");
        trinity.send(T.BUFFER_READY, new W().str(1, "t1").done());
        Msg wait = neo.await(T.BUFFER_WAIT, 3000);
        check("host is told who is still buffering",
                wait.str(1).equals("t1") && wait.all(2).size() == 1 && wait.all(2).get(0).equals(neoId),
                wait.str(1) + " " + wait.all(2));
        neo.send(T.BUFFER_READY, new W().str(1, "t1").done());
        Msg complete = neo.await(T.BUFFER_COMPLETE, 3000);
        check("host is told when everyone is ready", complete.str(1).equals("t1"), complete.str(1));

        // ── volume ──
        section("set_volume");
        neo.send(T.PLAYBACK_ACTION, new W().str(1, "set_volume").f32(8, 0.42f).done());
        Msg volumeEcho = trinity.await(T.SYNC_PLAYBACK, 3000);
        check("volume is relayed as a float",
                volumeEcho.str(1).equals("set_volume") && Math.abs(volumeEcho.f32(8) - 0.42f) < 0.001f,
                volumeEcho.str(1) + " " + volumeEcho.f32(8));
        trinity.send(T.REQUEST_SYNC, new byte[0]);
        Msg volumeState = trinity.await(T.SYNC_STATE, 3000);
        check("sync_state reports the new volume", Math.abs(volumeState.f32(6) - 0.42f) < 0.001f,
                "" + volumeState.f32(6));

        // ── suggestions ──
        section("track suggestions");
        trinity.send(T.SUGGEST_TRACK, new W().msg(1, track("t9", "Teenage Dream", "Katy Perry", 238_000)).done());
        Msg suggestion = neo.await(T.SUGGESTION_RECEIVED, 3000);
        String suggestionId = suggestion.str(1);
        check("host receives suggestion_received",
                !suggestionId.isEmpty() && suggestion.str(2).equals(trinityId) && suggestion.str(3).equals("Trinity"),
                suggestionId + " " + suggestion.str(3));
        Msg suggestedTrack = suggestion.msg(4);
        check("suggestion carries the track",
                suggestedTrack != null && suggestedTrack.str(1).equals("t9"),
                suggestedTrack == null ? "no track" : suggestedTrack.str(1));

        trinity.send(T.APPROVE_SUGGESTION, new W().str(1, suggestionId).done());
        Msg suggestDenied = trinity.await(T.ERROR, 3000);
        check("guests cannot approve suggestions", suggestDenied.str(1).equals("not_host"), suggestDenied.str(1));

        neo.send(T.APPROVE_SUGGESTION, new W().str(1, suggestionId).done());
        Msg approvedSuggestion = trinity.await(T.SUGGESTION_APPROVED, 3000);
        Msg alsoForHost = neo.await(T.SUGGESTION_APPROVED, 3000);
        check("everyone learns the suggestion was approved",
                approvedSuggestion.str(1).equals(suggestionId) && alsoForHost.str(1).equals(suggestionId),
                approvedSuggestion.str(1) + "/" + alsoForHost.str(1));

        trinity.send(T.SUGGEST_TRACK, new W().msg(1, track("t10", "Firework", "Katy Perry", 227_000)).done());
        Msg secondSuggestion = neo.await(T.SUGGESTION_RECEIVED, 3000);
        neo.send(T.REJECT_SUGGESTION, new W().str(1, secondSuggestion.str(1)).str(2, "not tonight").done());
        Msg rejectedSuggestion = trinity.await(T.SUGGESTION_REJECTED, 3000);
        check("the suggestor is told about a rejection",
                rejectedSuggestion.str(1).equals(secondSuggestion.str(1))
                        && rejectedSuggestion.str(2).equals("not tonight"),
                rejectedSuggestion.str(1) + " " + rejectedSuggestion.str(2));

        // ── reject a join request ──
        section("reject join request");
        Client cypher = new Client("late", host, port);
        cypher.send(T.JOIN_ROOM, new W().str(1, code).str(2, "Cypher").done());
        Msg cypherRequest = neo.await(T.JOIN_REQUEST, 3000);
        String cypherId = cypherRequest.str(1);
        neo.send(T.REJECT_JOIN, new W().str(1, cypherId).str(2, "room is full of cats").done());
        Msg cypherRejected = cypher.await(T.JOIN_REJECTED, 3000);
        check("a rejected guest is told why", cypherRejected.str(1).equals("room is full of cats"), cypherRejected.str(1));
        cypher.send(T.REQUEST_SYNC, new byte[0]);
        Msg cypherNotInRoom = cypher.await(T.ERROR, 3000);
        check("a rejected guest is not in the room", cypherNotInRoom.str(1).equals("not_in_room"), cypherNotInRoom.str(1));

        // ── kick ──
        section("kick");
        cypher.send(T.JOIN_ROOM, new W().str(1, code).str(2, "Cypher").done());
        Msg cypherRequest2 = neo.await(T.JOIN_REQUEST, 3000);
        neo.send(T.APPROVE_JOIN, new W().str(1, cypherRequest2.str(1)).done());
        Msg cypherApproved = cypher.await(T.JOIN_APPROVED, 3000);
        check("re-joining after a rejection works", cypherApproved.str(1).equals(code), cypherApproved.str(1));
        Msg cypherJoinedSeen = trinity.await(T.USER_JOINED, 3000);
        check("existing members see the new member",
                cypherJoinedSeen.str(1).equals(cypherRequest2.str(1)), cypherJoinedSeen.str(1));

        neo.send(T.KICK_USER, new W().str(1, cypherRequest2.str(1)).str(2, "listen to your own music").done());
        Msg kicked = cypher.await(T.KICKED, 3000);
        check("kicked guest gets the reason", kicked.str(1).equals("listen to your own music"), kicked.str(1));
        Msg cypherLeft = trinity.await(T.USER_LEFT, 3000);
        check("members see the kicked guest leave", cypherLeft.str(1).equals(cypherRequest2.str(1)), cypherLeft.str(1));
        cypher.send(T.RECONNECT, new W().str(1, cypher.str(2)).done());
        Msg kickRevoked = cypher.await(T.ERROR, 3000);
        check("a kicked session cannot be reused", kickRevoked.str(1).equals("session_not_found"), kickRevoked.str(1));
        cypher.close();

        // ── transfer host ──
        section("transfer host");
        neo.send(T.TRANSFER_HOST, new W().str(1, trinityId).done());
        Msg neoHostChanged = neo.await(T.HOST_CHANGED, 3000);
        Msg trinityHostChanged = trinity.await(T.HOST_CHANGED, 3000);
        check("everyone learns the new host",
                neoHostChanged.str(1).equals(trinityId) && trinityHostChanged.str(2).equals("Trinity"),
                neoHostChanged.str(1) + "/" + trinityHostChanged.str(2));
        trinity.send(T.PLAYBACK_ACTION, new W().str(1, "pause").str(2, "t1").alwaysI64(3, 9000).done());
        Msg pauseEcho = neo.await(T.SYNC_PLAYBACK, 3000);
        check("the new host can drive playback",
                pauseEcho.str(1).equals("pause") && pauseEcho.longVal(3) == 9000,
                pauseEcho.str(1) + " pos=" + pauseEcho.longVal(3));
        neo.send(T.PLAYBACK_ACTION, new W().str(1, "play").alwaysI64(3, 1).done());
        Msg demoted = neo.await(T.ERROR, 3000);
        check("the old host can no longer drive playback", demoted.str(1).equals("not_host"), demoted.str(1));

        // ── reconnect after a dropped socket ──
        section("disconnect + reconnect");
        trinity.close();
        Msg offlne = neo.await(T.USER_DISCONNECTED, 3000);
        check("members see a dropped peer", offlne.str(1).equals(trinityId), offlne.str(1));
        Client rejoin = new Client("reconnected", host, port);
        rejoin.send(T.RECONNECT, new W().str(1, trinityToken).done());
        Msg reconnected = rejoin.await(T.RECONNECTED, 3000);
        check("reconnect restores the session",
                reconnected.str(1).equals(code) && reconnected.str(2).equals(trinityId) && reconnected.bool(4),
                reconnected.str(1) + "/" + reconnected.str(2) + "/" + reconnected.bool(4));
        Msg reconnectState = reconnected.msg(3);
        check("reconnect replays the room state",
                reconnectState != null && reconnectState.str(1).equals(code)
                        && reconnectState.longVal(10) > 0,
                reconnectState == null ? "no state" : "rev=" + reconnectState.longVal(10));
        Msg backOnline = neo.await(T.USER_RECONNECTED, 3000);
        check("members see the peer come back", backOnline.str(1).equals(trinityId), backOnline.str(1));
        rejoin.send(T.RECONNECT, new W().str(1, "bogus-token").done());
        Msg bogus = rejoin.await(T.ERROR, 3000);
        check("an unknown session token is refused", bogus.str(1).equals("session_not_found"), bogus.str(1));

        // ── large frames (forces 64-bit frame lengths and gzip envelopes) ──
        section("large payloads");
        W big = new W().str(1, "change_track").str(2, "big-0").msg(4, track("big-0", "A Very Long Track Title", "An Artist", 300_000));
        for (int i = 0; i < 800; i++) {
            big.msg(6, track("big-" + i, "Track number " + i + " with a fairly long title", "Artist " + i, 200_000 + i));
        }
        byte[] bigPayload = big.done();
        rejoin.send(T.PLAYBACK_ACTION, bigPayload);
        Msg bigEcho = rejoin.await(T.SYNC_PLAYBACK, 15000);
        check("a >64 KiB queue survives the round trip", bigEcho.all(6).size() == 800, "" + bigEcho.all(6).size());
        check("the big relay was gzip compressed by the server", bigEcho.compressed, "not compressed");
        check("the big relay kept the track", bigEcho.msg(4) != null, "no track");
        neo.send(T.REQUEST_SYNC, new byte[0]);
        Msg bigState = neo.await(T.SYNC_STATE, 15000);
        check("a large sync_state round-trips", bigState.all(5).size() == 800, "" + bigState.all(5).size());

        // ── leave / unknown types / final stats ──
        section("leave + protocol edges");
        rejoin.send(T.LEAVE_ROOM, new byte[0]);
        Msg rejoinLeft = neo.await(T.USER_LEFT, 3000);
        check("leaving notifies the room", rejoinLeft.str(1).equals(trinityId), rejoinLeft.str(1));
        rejoin.send(T.REQUEST_SYNC, new byte[0]);
        Msg leftNotInRoom = rejoin.await(T.ERROR, 3000);
        check("a departed member is no longer in the room", leftNotInRoom.str(1).equals("not_in_room"), leftNotInRoom.str(1));
        rejoin.send(T.RECONNECT, new W().str(1, trinityToken).done());
        Msg leftSessionRevoked = rejoin.await(T.ERROR, 3000);
        check("leaving revokes the session", leftSessionRevoked.str(1).equals("session_not_found"), leftSessionRevoked.str(1));

        neo.send("definitely_not_a_message", new byte[0]);
        Msg unknown = neo.await(T.ERROR, 3000);
        check("unknown message types are refused politely", unknown.str(1).equals("unknown_type"), unknown.str(1));

        neo.send(T.BUFFER_READY, new byte[0]);
        check("an empty buffer_ready is ignored", neo.quiet(400) == null, "server answered an empty payload");

        String finalStats = httpGet(host, port);
        check("status endpoint reports the live room", jsonNumber(finalStats, "rooms") >= 1, String.valueOf(finalStats));
        check("status endpoint reports connected clients", jsonNumber(finalStats, "clients") >= 1, String.valueOf(finalStats));

        neo.close();
        rejoin.close();

        // ── summary ──
        System.out.println();
        System.out.println("passed: " + passed + "   failed: " + failed);
        if (failed > 0) {
            System.out.println("SMOKE TEST FAILED");
            System.exit(1);
        }
        System.out.println("SMOKE TEST PASSED");
    }

    // ══════════════════════════════════════════════════════════════════
    //  assertions
    // ══════════════════════════════════════════════════════════════════

    static void section(String title) {
        System.out.println("\n== " + title + " ==");
    }

    static void check(String label, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("  ok    " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label + "   -> " + detail);
        }
    }

    static boolean onlyRoomAlphabet(String code) {
        for (int i = 0; i < code.length(); i++) {
            if (ROOM_ALPHABET.indexOf(code.charAt(i)) < 0) return false;
        }
        return true;
    }

    static byte[] track(String id, String title, String artist, long duration) {
        return new W()
                .str(1, id)
                .str(2, title)
                .str(3, artist)
                .str(4, "Album " + id)
                .i64(5, duration)
                .str(6, "https://example.invalid/thumb/" + id)
                .done();
    }

    static String httpGet(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(3000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET / HTTP/1.1\r\nHost: " + host + ":" + port + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            InputStream in = socket.getInputStream();
            int b;
            while ((b = in.read()) >= 0) body.write(b);
            String text = body.toString(StandardCharsets.UTF_8);
            int split = text.indexOf("\r\n\r\n");
            return split < 0 ? text : text.substring(split + 4).trim();
        } catch (Exception e) {
            return "HTTP error: " + e;
        }
    }

    /** Reads a top-level numeric JSON field without pulling in a JSON library. */
    static double jsonNumber(String json, String key) {
        if (json == null) return 0;
        int at = json.indexOf("\"" + key + "\"");
        if (at < 0) return 0;
        int colon = json.indexOf(':', at);
        if (colon < 0) return 0;
        int end = colon + 1;
        while (end < json.length() && "-+.0123456789eE".indexOf(json.charAt(end)) >= 0) end++;
        try {
            return Double.parseDouble(json.substring(colon + 1, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  message type names
    // ══════════════════════════════════════════════════════════════════

    static final class T {
        static final String CREATE_ROOM = "create_room";
        static final String JOIN_ROOM = "join_room";
        static final String LEAVE_ROOM = "leave_room";
        static final String APPROVE_JOIN = "approve_join";
        static final String REJECT_JOIN = "reject_join";
        static final String PLAYBACK_ACTION = "playback_action";
        static final String BUFFER_READY = "buffer_ready";
        static final String KICK_USER = "kick_user";
        static final String TRANSFER_HOST = "transfer_host";
        static final String PING = "ping";
        static final String REQUEST_SYNC = "request_sync";
        static final String RECONNECT = "reconnect";
        static final String SUGGEST_TRACK = "suggest_track";
        static final String APPROVE_SUGGESTION = "approve_suggestion";
        static final String REJECT_SUGGESTION = "reject_suggestion";

        static final String ROOM_CREATED = "room_created";
        static final String JOIN_REQUEST = "join_request";
        static final String JOIN_APPROVED = "join_approved";
        static final String JOIN_REJECTED = "join_rejected";
        static final String USER_JOINED = "user_joined";
        static final String USER_LEFT = "user_left";
        static final String SYNC_PLAYBACK = "sync_playback";
        static final String BUFFER_WAIT = "buffer_wait";
        static final String BUFFER_COMPLETE = "buffer_complete";
        static final String ERROR = "error";
        static final String PONG = "pong";
        static final String HOST_CHANGED = "host_changed";
        static final String KICKED = "kicked";
        static final String SYNC_STATE = "sync_state";
        static final String RECONNECTED = "reconnected";
        static final String USER_RECONNECTED = "user_reconnected";
        static final String USER_DISCONNECTED = "user_disconnected";
        static final String SUGGESTION_RECEIVED = "suggestion_received";
        static final String SUGGESTION_APPROVED = "suggestion_approved";
        static final String SUGGESTION_REJECTED = "suggestion_rejected";

        private T() {
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  protobuf writer (test-side implementation)
    // ══════════════════════════════════════════════════════════════════

    static final class W {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream(128);

        private void varint(long value) {
            while (true) {
                if ((value & ~0x7FL) == 0) {
                    out.write((int) value);
                    return;
                }
                out.write((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }

        private void tag(int field, int wire) {
            varint(((long) field << 3) | wire);
        }

        W str(int field, String value) {
            if (value != null && !value.isEmpty()) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                tag(field, 2);
                varint(bytes.length);
                out.write(bytes, 0, bytes.length);
            }
            return this;
        }

        W msg(int field, byte[] value) {
            if (value != null) {
                tag(field, 2);
                varint(value.length);
                out.write(value, 0, value.length);
            }
            return this;
        }

        /** proto3 scalar: skipped when zero. */
        W i64(int field, long value) {
            if (value != 0) {
                tag(field, 0);
                varint(value);
            }
            return this;
        }

        /** Writes the field even when zero, like a client sending position 0. */
        W alwaysI64(int field, long value) {
            tag(field, 0);
            varint(value);
            return this;
        }

        W f32(int field, float value) {
            tag(field, 5);
            int bits = Float.floatToIntBits(value);
            for (int shift = 0; shift < 32; shift += 8) out.write((bits >>> shift) & 0xFF);
            return this;
        }

        byte[] done() {
            return out.toByteArray();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  protobuf reader (test-side implementation)
    // ══════════════════════════════════════════════════════════════════

    static final class R {
        private final byte[] data;
        private int pos;
        private int field;
        private int wire;

        R(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }

        boolean next() {
            if (pos >= data.length) return false;
            long key = readVarint();
            field = (int) (key >>> 3);
            wire = (int) (key & 7);
            return true;
        }

        int field() {
            return field;
        }

        long readVarint() {
            long result = 0;
            int shift = 0;
            while (true) {
                int b = data[pos++] & 0xFF;
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return result;
                shift += 7;
            }
        }

        String str() {
            int length = (int) readVarint();
            String value = new String(data, pos, length, StandardCharsets.UTF_8);
            pos += length;
            return value;
        }

        byte[] bytes() {
            int length = (int) readVarint();
            byte[] value = new byte[length];
            System.arraycopy(data, pos, value, 0, length);
            pos += length;
            return value;
        }

        float f32() {
            int bits = 0;
            for (int i = 0; i < 4; i++) bits |= (data[pos++] & 0xFF) << (8 * i);
            return Float.intBitsToFloat(bits);
        }

        void skip() {
            switch (wire) {
                case 0 -> readVarint();
                case 1 -> pos += 8;
                case 2 -> pos += (int) readVarint();
                case 5 -> pos += 4;
                default -> throw new IllegalStateException("unsupported wire type " + wire);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  decoded message
    // ══════════════════════════════════════════════════════════════════

    static final class Msg {
        final String type;
        final byte[] payload;
        final boolean compressed;

        Msg(String type, byte[] payload, boolean compressed) {
            this.type = type;
            this.payload = payload;
            this.compressed = compressed;
        }

        String str(int field) {
            R r = new R(payload);
            while (r.next()) {
                if (r.field() == field && r.wire == 2) return r.str();
                if (r.field() == field) r.skip();
                else r.skip();
            }
            return "";
        }

        long longVal(int field) {
            R r = new R(payload);
            while (r.next()) {
                if (r.field() == field && r.wire == 0) return r.readVarint();
                r.skip();
            }
            return 0;
        }

        float f32(int field) {
            R r = new R(payload);
            while (r.next()) {
                if (r.field() == field && r.wire == 5) return r.f32();
                r.skip();
            }
            return 0;
        }

        boolean bool(int field) {
            return longVal(field) != 0;
        }

        /** Last nested message with this field number. */
        Msg msg(int field) {
            Msg last = null;
            R r = new R(payload);
            while (r.next()) {
                if (r.field() == field && r.wire == 2) last = new Msg(type, r.bytes(), compressed);
                else r.skip();
            }
            return last;
        }

        List<String> all(int field) {
            List<String> out = new ArrayList<>();
            R r = new R(payload);
            while (r.next()) {
                if (r.field() == field && r.wire == 2) out.add(r.str());
                else r.skip();
            }
            return out;
        }

        final Present raw = new Present();

        /** Field-presence probe: proto3 scalars are absent when they were never written. */
        final class Present {
            boolean has(int field) {
                R r = new R(payload);
                while (r.next()) {
                    if (r.field() == field) return true;
                    r.skip();
                }
                return false;
            }
        }

        /** Reads a nested room state out of a message that embeds one. */
        Msg state(long timeoutField) {
            return this;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  WebSocket client
    // ══════════════════════════════════════════════════════════════════

    static final class Client implements Closeable {
        private static final Msg EOF = new Msg("$eof", new byte[0], false);

        private final String label;
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final Random random = new Random();
        private final LinkedBlockingQueue<Msg> inbox = new LinkedBlockingQueue<>();
        private final List<String> seen = new ArrayList<>();
        private volatile boolean closed;
        /** Last session token received, handy for reconnect assertions. */
        String sessionToken = "";

        Client(String label, String host, int port) throws IOException {
            this.label = label;
            this.socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setTcpNoDelay(true);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();

            byte[] nonce = new byte[16];
            new SecureRandom().nextBytes(nonce);
            String key = Base64.getEncoder().encodeToString(nonce);
            out.write(("GET /ws HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            String status = readLine();
            if (!status.contains("101")) throw new IOException(label + ": upgrade refused: " + status);
            while (true) {
                String header = readLine();
                if (header.isEmpty()) break;
            }
            Thread.ofVirtual().name("smoke-" + label).start(this::pump);
        }

        private void pump() {
            try {
                while (!closed) {
                    byte[] frame = readFrame();
                    if (frame == null) break;
                    inbox.offer(decodeEnvelope(frame));
                }
            } catch (Exception ignored) {
                // peer closed: fall through to the EOF marker
            }
            inbox.offer(EOF);
        }

        void send(String type, byte[] payload) throws IOException {
            byte[] envelope = encodeEnvelope(type, payload);
            int mask = random.nextInt();
            ByteArrayOutputStream frame = new ByteArrayOutputStream(envelope.length + 14);
            frame.write(0x82); // FIN + binary
            int length = envelope.length;
            if (length < 126) {
                frame.write(0x80 | length);
            } else if (length < 65_536) {
                frame.write(0x80 | 126);
                frame.write((length >>> 8) & 0xFF);
                frame.write(length & 0xFF);
            } else {
                frame.write(0x80 | 127);
                for (int shift = 56; shift >= 0; shift -= 8) frame.write((int) (((long) length >>> shift) & 0xFF));
            }
            frame.write((mask >>> 24) & 0xFF);
            frame.write((mask >>> 16) & 0xFF);
            frame.write((mask >>> 8) & 0xFF);
            frame.write(mask & 0xFF);
            for (int i = 0; i < envelope.length; i++) {
                frame.write(envelope[i] ^ ((mask >>> (8 * (3 - (i & 3)))) & 0xFF));
            }
            synchronized (out) {
                out.write(frame.toByteArray());
                out.flush();
            }
        }

        Msg await(String type, long timeoutMs) {
            Msg message = poll(type, timeoutMs);
            if (message == null) {
                throw new AssertionError(label + ": expected " + type + " but got nothing"
                        + (seen.isEmpty() ? "" : " (saw " + seen + ")"));
            }
            return message;
        }

        /** Returns the next message of this type, ignoring and remembering everything else. */
        Msg poll(String type, long timeoutMs) {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return null;
                Msg message;
                try {
                    message = inbox.poll(remaining, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                if (message == null) return null;
                if (message == EOF) {
                    seen.add("<closed>");
                    return null;
                }
                if (message.type.equals(type)) return message;
                seen.add(message.type);
            }
        }

        /** Any message within the window, used to prove the server stayed quiet. */
        Msg quiet(long timeoutMs) {
            Msg message = poll("$nothing", timeoutMs);
            if (message == null && !seen.isEmpty()) return new Msg(seen.get(seen.size() - 1), new byte[0], false);
            return message;
        }

        /** Waits for the next sync_state and unwraps its payload as a state view. */
        Msg state(long timeoutMs) {
            return await(T.SYNC_STATE, timeoutMs);
        }

        private String readLine() throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream(64);
            while (true) {
                int b = in.read();
                if (b < 0) throw new EOFException("closed during handshake");
                if (b == '\n') break;
                if (b != '\r') line.write(b);
            }
            return line.toString(StandardCharsets.UTF_8);
        }

        /** Returns the frame payload, or null when the peer sent a close frame. */
        private byte[] readFrame() throws IOException {
            int b0 = in.read();
            if (b0 < 0) return null;
            int b1 = in.read();
            if (b1 < 0) return null;
            int opcode = b0 & 0x0F;
            long length = b1 & 0x7F;
            if (length == 126) {
                length = ((long) readByte() << 8) | readByte();
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) length = (length << 8) | readByte();
            }
            byte[] mask = (b1 & 0x80) != 0 ? readN(4) : null;
            byte[] payload = readN((int) length);
            if (mask != null) {
                for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];
            }
            if (opcode == 0x9) {
                synchronized (out) {
                    out.write(new byte[]{(byte) 0x8A, (byte) 0x80, 0, 0, 0, 0});
                    out.flush();
                }
                return new byte[0];
            }
            if (opcode == 0x8) return null;
            if (opcode == 0xA) return new byte[0];
            return payload;
        }

        private int readByte() throws IOException {
            int b = in.read();
            if (b < 0) throw new EOFException("closed mid-frame");
            return b;
        }

        private byte[] readN(int n) throws IOException {
            if (n == 0) return new byte[0];
            byte[] buffer = new byte[n];
            int read = 0;
            while (read < n) {
                int r = in.read(buffer, read, n - read);
                if (r < 0) throw new EOFException("closed mid-frame");
                read += r;
            }
            return buffer;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try {
                synchronized (out) {
                    out.write(new byte[]{(byte) 0x88, (byte) 0x80, 0, 0, 0, 0});
                    out.flush();
                }
            } catch (IOException ignored) {
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  envelope codec (mirrors the contract the Android client uses)
    // ══════════════════════════════════════════════════════════════════

    static byte[] encodeEnvelope(String type, byte[] payload) throws IOException {
        byte[] body = payload == null ? new byte[0] : payload;
        boolean compressed = false;
        if (body.length > COMPRESSION_THRESHOLD) {
            byte[] gz = gzip(body);
            if (gz.length < body.length) {
                body = gz;
                compressed = true;
            }
        }
        W w = new W().str(1, type).msg(2, body);
        if (compressed) w.i64(3, 1);
        return w.done();
    }

    static Msg decodeEnvelope(byte[] data) throws IOException {
        String type = "";
        byte[] payload = new byte[0];
        boolean compressed = false;
        R r = new R(data);
        while (r.next()) {
            switch (r.field()) {
                case 1 -> type = r.str();
                case 2 -> payload = r.bytes();
                case 3 -> compressed = r.readVarint() != 0;
                default -> r.skip();
            }
        }
        if (compressed) {
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(payload))) {
                payload = gz.readAllBytes();
            }
        }
        return new Msg(type, payload, compressed);
    }

    static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 2 + 16);
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }

    private SmokeTest() {
    }
}
