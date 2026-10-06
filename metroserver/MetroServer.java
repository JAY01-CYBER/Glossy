/*
 * MetroServer — a self-hosted real-time backend for the "Listen Together"
 * feature of Glossy / Metrolist.
 *
 * It speaks exactly the protocol the Android client already implements:
 *   - RFC 6455 WebSocket, binary frames
 *   - each frame is a protobuf `Envelope { type, payload, compressed }`
 *   - payloads are the messages declared in metroproto/listentogether.proto,
 *     gzip-compressed whenever that makes them smaller (the client does the
 *     same above 100 bytes, and always accepts either form)
 *
 * There are no third-party dependencies: the WebSocket framing and the small
 * protobuf subset this protocol needs are implemented inline, so the entire
 * server is one file that runs on any JDK 21+.
 *
 * ── Run ─────────────────────────────────────────────────────────────────────
 *   java metroserver/MetroServer.java                # listens on :8080
 *   java metroserver/MetroServer.java 9000           # custom port
 *   PORT=9000 java metroserver/MetroServer.java      # or via env
 *
 * Then point the app at it:
 *   Settings → Integrations → Listen Together → Server URL
 *   ws://<host>:8080/ws
 *
 * Terminate TLS in front (nginx / Caddy / Cloudflare) and use wss:// — release
 * Android builds refuse cleartext sockets.
 *
 * Health / status:  GET http://<host>:8080/health
 * ────────────────────────────────────────────────────────────────────────────
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class MetroServer {

    // ───────────────────────────── tuning ─────────────────────────────
    static final int DEFAULT_PORT = 8080;
    /** Payloads above this size are gzipped (mirrors the client's codec). */
    static final int COMPRESSION_THRESHOLD = 100;
    static final int MAX_USERS_PER_ROOM = 16;
    static final int MAX_ROOMS = 5_000;
    /** How long a disconnected member's session stays resumable. */
    static final long SESSION_TTL_MS = 120_000L;
    /** Idle, empty rooms are reaped after this. */
    static final long EMPTY_ROOM_TTL_MS = 600_000L;
    /** A full queue sync can be a few hundred KB; anything larger is bogus. */
    static final int MAX_FRAME_BYTES = 4 * 1024 * 1024;
    static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    static final SecureRandom RANDOM = new SecureRandom();
    static final AtomicLong SUGGESTION_SEQ = new AtomicLong();

    // ══════════════════════════════════════════════════════════════════
    //  protobuf wire helpers (proto3, only the types this protocol uses)
    // ══════════════════════════════════════════════════════════════════

    static final class ProtoWriter {
        private final ByteArrayOutputStream out;

        ProtoWriter() {
            this(256);
        }

        ProtoWriter(int capacity) {
            out = new ByteArrayOutputStream(capacity);
        }

        void varint(long value) {
            long v = value;
            while (true) {
                int b = (int) (v & 0x7F);
                v >>>= 7;
                if (v == 0) {
                    out.write(b);
                    return;
                }
                out.write(b | 0x80);
            }
        }

        private void tag(int field, int wire) {
            varint(((long) field << 3) | wire);
        }

        /** proto3 omits scalar defaults, so empty strings are skipped. */
        void string(int field, String value) {
            if (value == null || value.isEmpty()) return;
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            tag(field, 2);
            varint(bytes.length);
            out.writeBytes(bytes);
        }

        void bytes(int field, byte[] value) {
            if (value == null || value.length == 0) return;
            tag(field, 2);
            varint(value.length);
            out.writeBytes(value);
        }

        /** Embedded message: same wire encoding as bytes. */
        void message(int field, byte[] value) {
            bytes(field, value);
        }

        void bool(int field, boolean value) {
            if (!value) return;
            tag(field, 0);
            varint(1L);
        }

        void int64(int field, long value) {
            if (value == 0L) return;
            tag(field, 0);
            varint(value);
        }

        void uint64(int field, long value) {
            int64(field, value);
        }

        void float32(int field, float value) {
            if (value == 0f) return;
            writeFloat(field, value);
        }

        /**
         * Writes a float even when it is 0. Needed for fields the client reads
         * as non-nullable: an omitted value would silently decode as 0 and
         * mute the listener.
         */
        void float32Always(int field, float value) {
            writeFloat(field, value);
        }

        private void writeFloat(int field, float value) {
            tag(field, 5);
            int bits = Float.floatToIntBits(value);
            out.write(bits & 0xFF);
            out.write((bits >>> 8) & 0xFF);
            out.write((bits >>> 16) & 0xFF);
            out.write((bits >>> 24) & 0xFF);
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }
    }

    static final class ProtoReader {
        private final byte[] buf;
        private int pos;
        private int field;
        private int wire;

        ProtoReader(byte[] buf) {
            this.buf = buf;
        }

        /** Advances to the next field; false at end of message. */
        boolean next() {
            if (pos >= buf.length) return false;
            long tag = readVarint();
            field = (int) (tag >>> 3);
            wire = (int) (tag & 0x7);
            return true;
        }

        int field() {
            return field;
        }

        int wire() {
            return wire;
        }

        long readVarint() {
            long result = 0;
            int shift = 0;
            while (true) {
                if (pos >= buf.length) throw new IllegalStateException("truncated varint");
                int b = buf[pos++] & 0xFF;
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return result;
                shift += 7;
                if (shift > 63) throw new IllegalStateException("varint too long");
            }
        }

        int readFixed32() {
            if (pos + 4 > buf.length) throw new IllegalStateException("truncated fixed32");
            int v = (buf[pos] & 0xFF)
                    | (buf[pos + 1] & 0xFF) << 8
                    | (buf[pos + 2] & 0xFF) << 16
                    | (buf[pos + 3] & 0xFF) << 24;
            pos += 4;
            return v;
        }

        String readString() {
            return new String(readBytes(), StandardCharsets.UTF_8);
        }

        byte[] readBytes() {
            int len = (int) readVarint();
            if (len < 0 || pos + len > buf.length) throw new IllegalStateException("truncated bytes");
            byte[] out = new byte[len];
            System.arraycopy(buf, pos, out, 0, len);
            pos += len;
            return out;
        }

        boolean readBool() {
            return readVarint() != 0L;
        }

        float readFloat() {
            return Float.intBitsToFloat(readFixed32());
        }

        void skip() {
            switch (wire) {
                case 0 -> readVarint();
                case 1 -> pos += 8;
                case 2 -> pos += (int) readVarint();
                case 5 -> pos += 4;
                default -> throw new IllegalStateException("unsupported wire type " + wire);
            }
            if (pos > buf.length) throw new IllegalStateException("truncated field");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  protocol constants (mirrors Protocol.kt / MessageTypes)
    // ══════════════════════════════════════════════════════════════════

    static final class T {
        // client -> server
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
        static final String CHAT = "chat";
        static final String REQUEST_SYNC = "request_sync";
        static final String RECONNECT = "reconnect";
        static final String SUGGEST_TRACK = "suggest_track";
        static final String APPROVE_SUGGESTION = "approve_suggestion";
        static final String REJECT_SUGGESTION = "reject_suggestion";

        // server -> client
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

        // playback actions
        static final String PLAY = "play";
        static final String PAUSE = "pause";
        static final String SEEK = "seek";
        static final String SKIP_NEXT = "skip_next";
        static final String SKIP_PREV = "skip_prev";
        static final String CHANGE_TRACK = "change_track";
        static final String QUEUE_ADD = "queue_add";
        static final String QUEUE_REMOVE = "queue_remove";
        static final String QUEUE_CLEAR = "queue_clear";
        static final String SYNC_QUEUE = "sync_queue";
        static final String SET_VOLUME = "set_volume";
    }

    // ══════════════════════════════════════════════════════════════════
    //  model
    // ══════════════════════════════════════════════════════════════════

    static final class Trk {
        String id = "";
        String title = "";
        String artist = "";
        String album = "";
        String thumbnail = "";
        String suggestedBy = "";
        long duration;

        byte[] toBytes() {
            ProtoWriter w = new ProtoWriter(192);
            w.string(1, id);
            w.string(2, title);
            w.string(3, artist);
            w.string(4, album);
            w.int64(5, duration);
            w.string(6, thumbnail);
            w.string(7, suggestedBy);
            return w.toByteArray();
        }

        static Trk read(byte[] data) {
            ProtoReader r = new ProtoReader(data);
            Trk t = new Trk();
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> t.id = r.readString();
                    case 2 -> t.title = r.readString();
                    case 3 -> t.artist = r.readString();
                    case 4 -> t.album = r.readString();
                    case 5 -> t.duration = r.readVarint();
                    case 6 -> t.thumbnail = r.readString();
                    case 7 -> t.suggestedBy = r.readString();
                    default -> r.skip();
                }
            }
            return t;
        }
    }

    static final class User {
        final String id;
        String username;
        boolean host;
        boolean connected = true;
        WsConn conn;

        User(String id, String username, boolean host) {
            this.id = id;
            this.username = username;
            this.host = host;
        }

        byte[] toBytes() {
            ProtoWriter w = new ProtoWriter(96);
            w.string(1, id);
            w.string(2, username);
            w.bool(3, host);
            w.bool(4, connected);
            return w.toByteArray();
        }
    }

    static final class Suggestion {
        final String id;
        final String fromUserId;
        final String fromUsername;
        final Trk track;

        Suggestion(String id, String fromUserId, String fromUsername, Trk track) {
            this.id = id;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.track = track;
        }
    }

    static final class Room {
        final String code;
        String hostId;
        final LinkedHashMap<String, User> users = new LinkedHashMap<>();
        /** userId -> username, awaiting host approval. */
        final LinkedHashMap<String, String> pendingJoins = new LinkedHashMap<>();
        final LinkedHashMap<String, Suggestion> suggestions = new LinkedHashMap<>();

        Trk currentTrack;
        boolean isPlaying;
        long position;
        long lastUpdate = now();
        float volume = 1f;
        final List<Trk> queue = new ArrayList<>();
        long revision;
        long emptySince;

        /** Track currently being buffered, plus the members already ready. */
        String bufferingTrackId;
        final Set<String> bufferedUsers = new LinkedHashSet<>();

        Room(String code) {
            this.code = code;
        }

        User host() {
            return users.get(hostId);
        }

        int connectedCount() {
            int n = 0;
            for (User u : users.values()) {
                if (u.connected) n++;
            }
            return n;
        }
    }

    static final class Session {
        final String token;
        final String roomCode;
        final String userId;
        final String username;
        volatile long expiresAt;

        Session(String token, String roomCode, String userId, String username) {
            this.token = token;
            this.roomCode = roomCode;
            this.userId = userId;
            this.username = username;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  message encoders (server -> client)
    // ══════════════════════════════════════════════════════════════════

    static final class Msgs {
        private Msgs() {
        }

        static byte[] roomCreated(String roomCode, String userId, String token) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, roomCode);
            w.string(2, userId);
            w.string(3, token);
            return w.toByteArray();
        }

        static byte[] joinRequest(String userId, String username) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, userId);
            w.string(2, username);
            return w.toByteArray();
        }

        static byte[] joinApproved(String userId, String token, Room room) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, room.code);
            w.string(2, userId);
            w.string(3, token);
            w.message(4, roomState(room));
            return w.toByteArray();
        }

        static byte[] joinRejected(String reason) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, reason);
            return w.toByteArray();
        }

        static byte[] userJoined(String userId, String username) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, userId);
            w.string(2, username);
            return w.toByteArray();
        }

        static byte[] userLeft(String userId, String username) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, userId);
            w.string(2, username);
            return w.toByteArray();
        }

        static byte[] bufferWait(String trackId, List<String> waitingFor) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, trackId);
            for (String id : waitingFor) w.string(2, id);
            return w.toByteArray();
        }

        static byte[] bufferComplete(String trackId) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, trackId);
            return w.toByteArray();
        }

        static byte[] error(String code, String message) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, code);
            w.string(2, message);
            return w.toByteArray();
        }

        static byte[] hostChanged(String newHostId, String newHostName) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, newHostId);
            w.string(2, newHostName);
            return w.toByteArray();
        }

        static byte[] kicked(String reason) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, reason);
            return w.toByteArray();
        }

        static byte[] syncState(Room room) {
            ProtoWriter w = new ProtoWriter();
            if (room.currentTrack != null) w.message(1, room.currentTrack.toBytes());
            w.bool(2, room.isPlaying);
            w.int64(3, effectivePosition(room));
            w.int64(4, room.lastUpdate);
            for (Trk t : room.queue) w.message(5, t.toBytes());
            w.float32Always(6, room.volume);
            w.uint64(7, room.revision);
            return w.toByteArray();
        }

        static byte[] pong(long clientTime, long serverReceiveTime, long serverSendTime, long sequence) {
            ProtoWriter w = new ProtoWriter();
            w.int64(1, clientTime);
            w.int64(2, serverReceiveTime);
            w.int64(3, serverSendTime);
            w.uint64(4, sequence);
            return w.toByteArray();
        }

        static byte[] reconnected(String userId, Room room, boolean isHost) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, room.code);
            w.string(2, userId);
            w.message(3, roomState(room));
            w.bool(4, isHost);
            return w.toByteArray();
        }

        static byte[] userReconnected(String userId, String username) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, userId);
            w.string(2, username);
            return w.toByteArray();
        }

        static byte[] userDisconnected(String userId, String username) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, userId);
            w.string(2, username);
            return w.toByteArray();
        }

        static byte[] suggestionReceived(Suggestion s) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, s.id);
            w.string(2, s.fromUserId);
            w.string(3, s.fromUsername);
            w.message(4, s.track.toBytes());
            return w.toByteArray();
        }

        static byte[] suggestionApproved(String suggestionId, Trk track) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, suggestionId);
            if (track != null) w.message(2, track.toBytes());
            return w.toByteArray();
        }

        static byte[] suggestionRejected(String suggestionId, String reason) {
            ProtoWriter w = new ProtoWriter();
            w.string(1, suggestionId);
            w.string(2, reason);
            return w.toByteArray();
        }

        static byte[] roomState(Room room) {
            ProtoWriter w = new ProtoWriter(512);
            w.string(1, room.code);
            w.string(2, room.hostId);
            for (User u : room.users.values()) w.message(3, u.toBytes());
            if (room.currentTrack != null) w.message(4, room.currentTrack.toBytes());
            w.bool(5, room.isPlaying);
            w.int64(6, effectivePosition(room));
            w.int64(7, room.lastUpdate);
            w.float32Always(8, room.volume);
            for (Trk t : room.queue) w.message(9, t.toBytes());
            w.uint64(10, room.revision);
            return w.toByteArray();
        }

        /** Broadcast form of a host playback action, stamped with a revision. */
        static byte[] syncPlayback(
                String action,
                Trk track,
                String trackId,
                long position,
                boolean includePosition,
                Boolean insertNext,
                List<Trk> queue,
                String queueTitle,
                Float volume,
                long serverTime,
                long revision) {
            ProtoWriter w = new ProtoWriter(512);
            w.string(1, action);
            w.string(2, trackId);
            if (includePosition) w.int64(3, position);
            if (track != null) w.message(4, track.toBytes());
            if (insertNext != null) w.bool(5, insertNext);
            if (queue != null) for (Trk t : queue) w.message(6, t.toBytes());
            w.string(7, queueTitle);
            if (volume != null) w.float32Always(8, volume);
            w.int64(9, serverTime);
            w.uint64(10, revision);
            return w.toByteArray();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  WebSocket (RFC 6455)
    // ══════════════════════════════════════════════════════════════════

    static final class WsConn {
        final Socket socket;
        final InputStream in;
        final OutputStream out;
        final String remote;
        volatile boolean open = true;

        // binding
        Room room;
        String userId;
        String pendingUserId;

        WsConn(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.remote = String.valueOf(socket.getRemoteSocketAddress());
        }

        synchronized void send(String type, byte[] payload) {
            if (!open) return;
            try {
                writeFrame(0x2, envelope(type, payload));
            } catch (IOException e) {
                open = false;
            }
        }

        private void writeFrame(int opcode, byte[] payload) throws IOException {
            ByteArrayOutputStream header = new ByteArrayOutputStream(10);
            header.write(0x80 | opcode);
            int len = payload.length;
            if (len < 126) {
                header.write(len);
            } else if (len < 65_536) {
                header.write(126);
                header.write((len >>> 8) & 0xFF);
                header.write(len & 0xFF);
            } else {
                header.write(127);
                for (int shift = 56; shift >= 0; shift -= 8) {
                    header.write((int) (((long) len >>> shift) & 0xFF));
                }
            }
            out.write(header.toByteArray());
            out.write(payload);
            out.flush();
        }

        private void writeControlFrame(int opcode, byte[] payload) {
            if (!open) return;
            try {
                writeFrame(opcode, payload);
            } catch (IOException e) {
                open = false;
            }
        }

        /**
         * Reads the next complete application message, handling fragmentation
         * and control frames. Returns null when the peer closed.
         */
        byte[] readMessage() throws IOException {
            ByteArrayOutputStream fragmented = null;
            while (true) {
                int b0 = readByte();
                int b1 = readByte();
                boolean fin = (b0 & 0x80) != 0;
                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long len = b1 & 0x7F;
                if (len == 126) {
                    len = ((long) readByte() << 8) | readByte();
                } else if (len == 127) {
                    len = 0;
                    for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
                }
                if (len < 0 || len > MAX_FRAME_BYTES) throw new IOException("frame too large: " + len);
                byte[] mask = masked ? readN(4) : null;
                byte[] payload = readN((int) len);
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ mask[i & 3]);
                    }
                }

                switch (opcode) {
                    case 0x0 -> { // continuation
                        if (fragmented != null) {
                            fragmented.write(payload);
                            if (fin) return fragmented.toByteArray();
                        }
                    }
                    case 0x1, 0x2 -> {
                        if (fin) return payload;
                        fragmented = new ByteArrayOutputStream(Math.max(64, payload.length * 2));
                        fragmented.write(payload);
                    }
                    case 0x8 -> { // close
                        writeControlFrame(0x8, new byte[0]);
                        open = false;
                        return null;
                    }
                    case 0x9 -> writeControlFrame(0xA, payload); // ping -> pong
                    case 0xA -> { /* pong: the app never needs it */ }
                    default -> { /* reserved opcodes */ }
                }
            }
        }

        private int readByte() throws IOException {
            int b = in.read();
            if (b < 0) throw new EOFException("connection closed");
            return b;
        }

        private byte[] readN(int n) throws IOException {
            if (n == 0) return new byte[0];
            byte[] buf = new byte[n];
            int read = 0;
            while (read < n) {
                int r = in.read(buf, read, n - read);
                if (r < 0) throw new EOFException("connection closed mid-frame");
                read += r;
            }
            return buf;
        }

        void close() {
            open = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  envelope codec
    // ══════════════════════════════════════════════════════════════════

    static final class Envelope {
        String type = "";
        byte[] payload = new byte[0];
    }

    static byte[] envelope(String type, byte[] payload) {
        byte[] body = payload == null ? new byte[0] : payload;
        boolean compressed = false;
        if (body.length > COMPRESSION_THRESHOLD) {
            byte[] gz = gzip(body);
            if (gz.length < body.length) {
                body = gz;
                compressed = true;
            }
        }
        ProtoWriter w = new ProtoWriter(body.length + 32);
        w.string(1, type);
        w.bytes(2, body);
        w.bool(3, compressed);
        return w.toByteArray();
    }

    static Envelope parseEnvelope(byte[] data) {
        Envelope env = new Envelope();
        ProtoReader r = new ProtoReader(data);
        boolean compressed = false;
        while (r.next()) {
            switch (r.field()) {
                case 1 -> env.type = r.readString();
                case 2 -> env.payload = r.readBytes();
                case 3 -> compressed = r.readBool();
                default -> r.skip();
            }
        }
        if (compressed && env.payload.length > 0) {
            byte[] raw = gunzip(env.payload);
            if (raw != null) env.payload = raw;
        }
        return env;
    }

    static byte[] gzip(byte[] data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 2 + 16);
            try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
                gz.write(data);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return data;
        }
    }

    static byte[] gunzip(byte[] data) {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  hub — rooms, sessions and message handling
    // ══════════════════════════════════════════════════════════════════

    static final class Hub {
        final Map<String, Room> rooms = new LinkedHashMap<>();
        final Map<String, Session> sessions = new LinkedHashMap<>();
        /** Join requests awaiting host approval: userId -> connection. */
        final Map<String, WsConn> pendingConnections = new LinkedHashMap<>();
        final Timer timer = new Timer("metroserver-janitor", true);
        final long startedAt = now();

        // ── entry point ──

        synchronized void handle(String type, byte[] payload, WsConn conn) {
            try {
                switch (type) {
                    case T.CREATE_ROOM -> createRoom(payload, conn);
                    case T.JOIN_ROOM -> joinRoom(payload, conn);
                    case T.APPROVE_JOIN -> approveJoin(payload, conn);
                    case T.REJECT_JOIN -> rejectJoin(payload, conn);
                    case T.PLAYBACK_ACTION -> playbackAction(payload, conn);
                    case T.BUFFER_READY -> bufferReady(payload, conn);
                    case T.KICK_USER -> kickUser(payload, conn);
                    case T.TRANSFER_HOST -> transferHost(payload, conn);
                    case T.PING -> ping(payload, conn);
                    case T.REQUEST_SYNC -> requestSync(conn);
                    case T.RECONNECT -> reconnect(payload, conn);
                    case T.LEAVE_ROOM -> leaveRoom(conn, "left");
                    case T.SUGGEST_TRACK -> suggestTrack(payload, conn);
                    case T.APPROVE_SUGGESTION -> approveSuggestion(payload, conn);
                    case T.REJECT_SUGGESTION -> rejectSuggestion(payload, conn);
                    case T.CHAT -> { /* reserved: the client has no chat UI yet */ }
                    default -> conn.send(T.ERROR, Msgs.error("unknown_type", "Unsupported message: " + type));
                }
            } catch (Exception e) {
                conn.send(T.ERROR, Msgs.error("bad_payload", "Could not handle " + type + ": " + e));
            }
        }

        // ── room lifecycle ──

        private void createRoom(byte[] payload, WsConn conn) {
            String username = username(payload, "Listener");
            if (conn.room != null) leaveRoom(conn, "new room");
            if (rooms.size() >= MAX_ROOMS) {
                conn.send(T.ERROR, Msgs.error("server_full", "Server is at capacity"));
                return;
            }

            String code = newRoomCode();
            Room room = new Room(code);
            User host = new User(UUID.randomUUID().toString(), username, true);
            host.conn = conn;
            room.users.put(host.id, host);
            room.hostId = host.id;
            room.lastUpdate = now();
            rooms.put(code, room);

            String token = issueSession(room, host);
            conn.room = room;
            conn.userId = host.id;
            conn.pendingUserId = null;

            conn.send(T.ROOM_CREATED, Msgs.roomCreated(code, host.id, token));
            log("room created", code + " by " + username);
        }

        private void joinRoom(byte[] payload, WsConn conn) {
            String code = "";
            String username = "Listener";
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> code = r.readString().toUpperCase();
                    case 2 -> username = r.readString();
                    default -> r.skip();
                }
            }

            if (conn.room != null) leaveRoom(conn, "rejoined");

            Room room = rooms.get(code);
            if (room == null) {
                conn.send(T.JOIN_REJECTED, Msgs.joinRejected("Room not found"));
                return;
            }
            if (room.users.size() + room.pendingJoins.size() >= MAX_USERS_PER_ROOM) {
                conn.send(T.JOIN_REJECTED, Msgs.joinRejected("Room is full"));
                return;
            }

            User host = room.host();
            if (host == null || host.conn == null || !host.conn.open) {
                conn.send(T.JOIN_REJECTED, Msgs.joinRejected("Host is offline"));
                return;
            }

            String userId = UUID.randomUUID().toString();
            room.pendingJoins.put(userId, username);
            pendingConnections.put(userId, conn);
            conn.room = room;
            conn.userId = null;
            conn.pendingUserId = userId;

            host.conn.send(T.JOIN_REQUEST, Msgs.joinRequest(userId, username));
            log("join requested", code + " <- " + username);
        }

        private void approveJoin(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;
            String userId = firstString(payload);
            String username = room.pendingJoins.remove(userId);
            if (username == null) {
                conn.send(T.ERROR, Msgs.error("no_such_request", "No pending join for " + userId));
                return;
            }
            WsConn guestConn = pendingConnections.remove(userId);

            User user = new User(userId, username, false);
            user.conn = guestConn;
            room.users.put(userId, user);
            String token = issueSession(room, user);

            if (guestConn != null) {
                guestConn.room = room;
                guestConn.userId = userId;
                guestConn.pendingUserId = null;
                guestConn.send(T.JOIN_APPROVED, Msgs.joinApproved(userId, token, room));
                log("join approved", room.code + " <- " + username);
            }

            // Everyone already in the room (host included) learns about the new member.
            broadcastExcept(room, userId, T.USER_JOINED, Msgs.userJoined(userId, username));
        }

        private void rejectJoin(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;

            String userId = "";
            String reason = "Request declined";
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> userId = r.readString();
                    case 2 -> {
                        String value = r.readString();
                        if (!value.isEmpty()) reason = value;
                    }
                    default -> r.skip();
                }
            }

            room.pendingJoins.remove(userId);
            WsConn guestConn = pendingConnections.remove(userId);
            if (guestConn != null) {
                guestConn.room = null;
                guestConn.pendingUserId = null;
                guestConn.send(T.JOIN_REJECTED, Msgs.joinRejected(reason));
            }
        }

        private void leaveRoom(WsConn conn, String reason) {
            Room room = conn.room;
            String userId = conn.userId;
            String pendingId = conn.pendingUserId;
            conn.room = null;
            conn.userId = null;
            conn.pendingUserId = null;
            if (room == null) return;

            if (userId == null) {
                // Never approved: just drop the pending request.
                if (pendingId != null) {
                    room.pendingJoins.remove(pendingId);
                    pendingConnections.remove(pendingId);
                }
                return;
            }

            User user = room.users.remove(userId);
            revokeSession(room.code, userId);
            if (user == null) return;

            broadcastExcept(room, userId, T.USER_LEFT, Msgs.userLeft(userId, user.username));
            log("user left", room.code + " " + user.username + " (" + reason + ")");

            if (user.host || room.hostId.equals(userId)) promoteNewHost(room);
            maybeDropRoom(room);
        }

        private void maybeDropRoom(Room room) {
            if (!room.users.isEmpty()) return;
            for (Session s : sessions.values()) {
                if (s.roomCode.equals(room.code)) return;
            }
            rooms.remove(room.code);
            log("room closed", room.code);
        }

        private void promoteNewHost(Room room) {
            for (User candidate : room.users.values()) {
                if (candidate.conn == null || !candidate.conn.open) continue;
                for (User u : room.users.values()) u.host = u.id.equals(candidate.id);
                room.hostId = candidate.id;
                broadcastAll(room, T.HOST_CHANGED, Msgs.hostChanged(candidate.id, candidate.username));
                log("host promoted", room.code + " -> " + candidate.username);
                return;
            }
            // Nobody connected to take over; the room will be reaped.
        }

        // ── playback ──

        private void playbackAction(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;

            String action = "";
            String trackId = "";
            long position = 0;
            boolean hasPosition = false;
            Trk track = null;
            boolean insertNext = false;
            List<Trk> queue = null;
            String queueTitle = "";
            Float volume = null;
            long capturedAt = 0;

            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> action = r.readString();
                    case 2 -> trackId = r.readString();
                    case 3 -> {
                        position = r.readVarint();
                        hasPosition = true;
                    }
                    case 4 -> track = Trk.read(r.readBytes());
                    case 5 -> insertNext = r.readBool();
                    case 6 -> {
                        if (queue == null) queue = new ArrayList<>();
                        queue.add(Trk.read(r.readBytes()));
                    }
                    case 7 -> queueTitle = r.readString();
                    case 8 -> volume = r.readFloat();
                    case 9 -> r.readVarint();  // client server_time: the server owns the clock
                    case 10 -> r.readVarint(); // client revision: the server owns revisions
                    case 11 -> capturedAt = r.readVarint();
                    default -> r.skip();
                }
            }

            long serverTime = capturedAt > 0 ? capturedAt : now();
            boolean isPositionAction =
                    action.equals(T.PLAY) || action.equals(T.PAUSE) || action.equals(T.SEEK);

            switch (action) {
                case T.PLAY -> {
                    room.isPlaying = true;
                    if (hasPosition) room.position = position;
                    room.lastUpdate = serverTime;
                }
                case T.PAUSE -> {
                    room.isPlaying = false;
                    if (hasPosition) room.position = position;
                    room.lastUpdate = serverTime;
                }
                case T.SEEK -> {
                    if (hasPosition) room.position = position;
                    room.lastUpdate = serverTime;
                }
                case T.CHANGE_TRACK, T.SKIP_NEXT, T.SKIP_PREV -> {
                    if (track != null) room.currentTrack = track;
                    if (queue != null) {
                        room.queue.clear();
                        room.queue.addAll(queue);
                    }
                    room.isPlaying = false;
                    room.position = 0;
                    room.lastUpdate = serverTime;
                    resetBuffering(room);
                }
                case T.QUEUE_ADD -> {
                    if (queue != null) {
                        room.queue.clear();
                        room.queue.addAll(queue);
                    } else if (track != null) {
                        if (insertNext) room.queue.add(0, track);
                        else room.queue.add(track);
                    }
                }
                case T.QUEUE_REMOVE -> {
                    if (queue != null) {
                        room.queue.clear();
                        room.queue.addAll(queue);
                    } else if (!trackId.isEmpty()) {
                        final String removeId = trackId;
                        room.queue.removeIf(t -> t.id.equals(removeId));
                    }
                }
                case T.QUEUE_CLEAR -> {
                    room.queue.clear();
                    if (queue != null) room.queue.addAll(queue);
                }
                case T.SYNC_QUEUE -> {
                    room.queue.clear();
                    if (queue != null) room.queue.addAll(queue);
                }
                case T.SET_VOLUME -> room.volume = volume == null ? 1f : clamp(volume);
                default -> {
                    // Unknown/newer action: relayed as-is so clients stay compatible.
                }
            }

            room.revision++;
            long revision = room.revision;

            byte[] relay = Msgs.syncPlayback(
                    action,
                    track,
                    trackId.isEmpty() ? null : trackId,
                    isPositionAction ? (hasPosition ? position : room.position) : position,
                    isPositionAction || hasPosition,
                    insertNext ? Boolean.TRUE : null,
                    queue,
                    queueTitle,
                    action.equals(T.SET_VOLUME) ? room.volume : null,
                    serverTime,
                    revision);

            for (User user : room.users.values()) {
                if (user.connected && user.conn != null && !user.id.equals(conn.userId)) {
                    user.conn.send(T.SYNC_PLAYBACK, relay);
                }
            }
        }

        private void resetBuffering(Room room) {
            room.bufferingTrackId = null;
            room.bufferedUsers.clear();
        }

        private void bufferReady(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (room == null || conn.userId == null) return;
            String trackId = firstString(payload);
            if (trackId.isEmpty()) return;

            if (!trackId.equals(room.bufferingTrackId)) {
                room.bufferingTrackId = trackId;
                room.bufferedUsers.clear();
            }
            room.bufferedUsers.add(conn.userId);

            List<String> waitingFor = new ArrayList<>();
            for (User user : room.users.values()) {
                if (user.connected && !room.bufferedUsers.contains(user.id)) {
                    waitingFor.add(user.id);
                }
            }

            User host = room.host();
            if (host == null || host.conn == null || !host.conn.open) return;
            if (waitingFor.isEmpty()) {
                host.conn.send(T.BUFFER_COMPLETE, Msgs.bufferComplete(trackId));
            } else {
                host.conn.send(T.BUFFER_WAIT, Msgs.bufferWait(trackId, waitingFor));
            }
        }

        private void requestSync(WsConn conn) {
            Room room = conn.room;
            if (room == null || conn.userId == null) {
                conn.send(T.ERROR, Msgs.error("not_in_room", "Join a room first"));
                return;
            }
            conn.send(T.SYNC_STATE, Msgs.syncState(room));
        }

        // ── moderation ──

        private void kickUser(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;

            String userId = "";
            String reason = "";
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> userId = r.readString();
                    case 2 -> reason = r.readString();
                    default -> r.skip();
                }
            }

            User target = room.users.remove(userId);
            if (target == null) return;
            revokeSession(room.code, userId);

            WsConn targetConn = target.conn;
            if (targetConn != null) {
                targetConn.send(T.KICKED, Msgs.kicked(reason.isEmpty() ? "Kicked by host" : reason));
                targetConn.room = null;
                targetConn.userId = null;
            }
            broadcastExcept(room, userId, T.USER_LEFT, Msgs.userLeft(userId, target.username));
            log("user kicked", room.code + " " + target.username);
            maybeDropRoom(room);
        }

        private void transferHost(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;
            String newHostId = firstString(payload);
            User target = room.users.get(newHostId);
            if (target == null) {
                conn.send(T.ERROR, Msgs.error("no_such_user", "Unknown user " + newHostId));
                return;
            }
            for (User u : room.users.values()) u.host = u.id.equals(newHostId);
            room.hostId = newHostId;
            broadcastAll(room, T.HOST_CHANGED, Msgs.hostChanged(newHostId, target.username));
            log("host transferred", room.code + " -> " + target.username);
        }

        // ── suggestions ──

        private void suggestTrack(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (room == null || conn.userId == null) {
                conn.send(T.ERROR, Msgs.error("not_in_room", "Join a room first"));
                return;
            }
            Trk track = null;
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                if (r.field() == 1) track = Trk.read(r.readBytes());
                else r.skip();
            }
            if (track == null) return;

            User sender = room.users.get(conn.userId);
            if (sender == null) return;

            String id = "sg_" + SUGGESTION_SEQ.incrementAndGet() + "_" + Integer.toHexString(RANDOM.nextInt());
            Suggestion suggestion = new Suggestion(id, sender.id, sender.username, track);
            room.suggestions.put(id, suggestion);

            User host = room.host();
            if (host != null && host.conn != null && host.conn.open) {
                host.conn.send(T.SUGGESTION_RECEIVED, Msgs.suggestionReceived(suggestion));
            }
        }

        private void approveSuggestion(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;
            Suggestion suggestion = room.suggestions.remove(firstString(payload));
            if (suggestion == null) return;
            // Everyone learns the outcome; the host then queues it via queue_add.
            broadcastAll(room, T.SUGGESTION_APPROVED, Msgs.suggestionApproved(suggestion.id, suggestion.track));
        }

        private void rejectSuggestion(byte[] payload, WsConn conn) {
            Room room = conn.room;
            if (!requireHost(conn, room)) return;

            String id = "";
            String reason = "";
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> id = r.readString();
                    case 2 -> reason = r.readString();
                    default -> r.skip();
                }
            }

            Suggestion suggestion = room.suggestions.remove(id);
            if (suggestion == null) return;
            User sender = room.users.get(suggestion.fromUserId);
            if (sender != null && sender.conn != null && sender.conn.open) {
                sender.conn.send(T.SUGGESTION_REJECTED, Msgs.suggestionRejected(id, reason));
            }
        }

        // ── clock sync ──

        private void ping(byte[] payload, WsConn conn) {
            long clientTime = 0;
            long sequence = 0;
            ProtoReader r = new ProtoReader(payload);
            while (r.next()) {
                switch (r.field()) {
                    case 1 -> clientTime = r.readVarint();
                    case 2 -> sequence = r.readVarint();
                    default -> r.skip();
                }
            }
            long receive = now();
            conn.send(T.PONG, Msgs.pong(clientTime, receive, now(), sequence));
        }

        // ── reconnection ──

        private void reconnect(byte[] payload, WsConn conn) {
            Session session = sessions.get(firstString(payload));
            Room room = session == null ? null : rooms.get(session.roomCode);
            if (session == null || room == null || session.expiresAt < now()) {
                if (session != null) sessions.remove(session.token);
                conn.send(T.ERROR, Msgs.error("session_not_found", "Session expired or unknown"));
                return;
            }

            User user = room.users.get(session.userId);
            if (user == null) {
                sessions.remove(session.token);
                conn.send(T.ERROR, Msgs.error("session_not_found", "You are no longer in this room"));
                return;
            }

            if (user.conn != null && user.conn != conn) {
                user.conn.send(T.ERROR, Msgs.error("replaced", "Connected from another device"));
                user.conn.close();
            }
            user.conn = conn;
            user.connected = true;
            session.expiresAt = now() + SESSION_TTL_MS;

            conn.room = room;
            conn.userId = user.id;
            conn.pendingUserId = null;

            conn.send(T.RECONNECTED, Msgs.reconnected(user.id, room, user.host));
            broadcastExcept(room, user.id, T.USER_RECONNECTED, Msgs.userReconnected(user.id, user.username));
            log("reconnected", room.code + " " + user.username);
        }

        // ── session bookkeeping ──

        private String issueSession(Room room, User user) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, new Session(token, room.code, user.id, user.username));
            return token;
        }

        private void revokeSession(String roomCode, String userId) {
            sessions.values().removeIf(s -> s.roomCode.equals(roomCode) && s.userId.equals(userId));
        }

        // ── disconnect ──

        synchronized void handleDisconnect(WsConn conn) {
            Room room = conn.room;
            String userId = conn.userId;
            String pendingId = conn.pendingUserId;

            if (room == null) {
                if (pendingId != null) pendingConnections.remove(pendingId);
                return;
            }
            if (userId == null) {
                if (pendingId != null) {
                    room.pendingJoins.remove(pendingId);
                    pendingConnections.remove(pendingId);
                }
                return;
            }

            User user = room.users.get(userId);
            if (user == null || user.conn != conn) return;

            user.connected = false;
            user.conn = null;
            broadcastExcept(room, userId, T.USER_DISCONNECTED, Msgs.userDisconnected(userId, user.username));
            log("user disconnected", room.code + " " + user.username);

            for (Session s : sessions.values()) {
                if (s.roomCode.equals(room.code) && s.userId.equals(userId)) {
                    s.expiresAt = now() + SESSION_TTL_MS;
                }
            }
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    expire(userId, room);
                }
            }, SESSION_TTL_MS);
        }

        /** Removes a member whose session expired without reconnecting. */
        private synchronized void expire(String userId, Room room) {
            User user = room.users.get(userId);
            if (user == null || user.connected) return;
            room.users.remove(userId);
            revokeSession(room.code, userId);
            broadcastExcept(room, userId, T.USER_LEFT, Msgs.userLeft(userId, user.username));
            log("session expired", room.code + " " + user.username);
            if (user.host || room.hostId.equals(userId)) promoteNewHost(room);
            maybeDropRoom(room);
        }

        // ── helpers ──

        private boolean requireHost(WsConn conn, Room room) {
            if (room == null) {
                conn.send(T.ERROR, Msgs.error("not_in_room", "Join a room first"));
                return false;
            }
            if (conn.userId == null || !conn.userId.equals(room.hostId)) {
                conn.send(T.ERROR, Msgs.error("not_host", "Only the host can do that"));
                return false;
            }
            return true;
        }

        private void broadcastAll(Room room, String type, byte[] payload) {
            for (User user : room.users.values()) {
                if (user.connected && user.conn != null && user.conn.open) {
                    user.conn.send(type, payload);
                }
            }
        }

        private void broadcastExcept(Room room, String exceptUserId, String type, byte[] payload) {
            for (User user : room.users.values()) {
                if (user.id.equals(exceptUserId)) continue;
                if (user.connected && user.conn != null && user.conn.open) {
                    user.conn.send(type, payload);
                }
            }
        }

        private String newRoomCode() {
            String code;
            do {
                StringBuilder sb = new StringBuilder(6);
                for (int i = 0; i < 6; i++) {
                    sb.append(ROOM_ALPHABET.charAt(RANDOM.nextInt(ROOM_ALPHABET.length())));
                }
                code = sb.toString();
            } while (rooms.containsKey(code));
            return code;
        }

        synchronized void reap() {
            long cutoff = now() - EMPTY_ROOM_TTL_MS;
            Iterator<Map.Entry<String, Room>> it = rooms.entrySet().iterator();
            while (it.hasNext()) {
                Room room = it.next().getValue();
                if (room.connectedCount() > 0) {
                    room.emptySince = 0L;
                    continue;
                }
                if (room.emptySince == 0L) {
                    room.emptySince = now();
                    continue;
                }
                if (room.emptySince < cutoff) {
                    boolean hasSession = false;
                    for (Session s : sessions.values()) {
                        if (s.roomCode.equals(room.code)) {
                            hasSession = true;
                            break;
                        }
                    }
                    if (!hasSession) {
                        it.remove();
                        log("room reaped", room.code);
                    }
                }
            }
        }

        synchronized String stats() {
            int clients = 0;
            for (Room room : rooms.values()) clients += room.connectedCount();
            return "{\"status\":\"ok\",\"rooms\":" + rooms.size()
                    + ",\"clients\":" + clients
                    + ",\"sessions\":" + sessions.size()
                    + ",\"uptimeSeconds\":" + ((now() - startedAt) / 1000L) + "}";
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  small shared helpers
    // ══════════════════════════════════════════════════════════════════

    static long now() {
        return System.currentTimeMillis();
    }

    static long effectivePosition(Room room) {
        if (!room.isPlaying) return room.position;
        long delta = now() - room.lastUpdate;
        return delta > 0 ? room.position + delta : room.position;
    }

    static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** Reads field #1 of a message as a string ("" when absent). */
    static String firstString(byte[] payload) {
        ProtoReader r = new ProtoReader(payload);
        String value = "";
        while (r.next()) {
            if (r.field() == 1 && r.wire() == 2) value = r.readString();
            else r.skip();
        }
        return value;
    }

    static String username(byte[] payload, String fallback) {
        String value = firstString(payload);
        return value.isEmpty() ? fallback : value;
    }

    static void log(String what, String detail) {
        System.out.println("[" + java.time.LocalTime.now().withNano(0) + "] " + what + ": " + detail);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HTTP upgrade + accept loop
    // ══════════════════════════════════════════════════════════════════

    /** Reads one header line byte-at-a-time so no body bytes are swallowed. */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream(64);
        while (true) {
            int b = in.read();
            if (b < 0) throw new EOFException("connection closed during handshake");
            if (b == '\n') break;
            if (b != '\r') line.write(b);
            if (line.size() > 8192) throw new IOException("header line too long");
        }
        return line.toString(StandardCharsets.UTF_8);
    }

    /** Returns true when the socket was upgraded to a WebSocket. */
    static boolean performHandshake(Socket socket, InputStream in, OutputStream out, Hub hub) throws IOException {
        String requestLine = readLine(in);
        String key = null;
        while (true) {
            String header = readLine(in);
            if (header.isEmpty()) break;
            int colon = header.indexOf(':');
            if (colon <= 0) continue;
            if (header.substring(0, colon).trim().equalsIgnoreCase("Sec-WebSocket-Key")) {
                key = header.substring(colon + 1).trim();
            }
        }

        if (key == null) {
            // Plain HTTP: serve a tiny status page (handy for uptime probes).
            byte[] body = hub.stats().getBytes(StandardCharsets.UTF_8);
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                    + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
            return false;
        }

        String accept = Base64.getEncoder()
                .encodeToString(sha1((key + WS_MAGIC).getBytes(StandardCharsets.UTF_8)));
        out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        return true;
    }

    static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void serve(Socket socket, Hub hub) {
        WsConn conn = null;
        try {
            socket.setTcpNoDelay(true);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            if (!performHandshake(socket, in, out, hub)) {
                socket.close();
                return;
            }
            conn = new WsConn(socket, in, out);
            log("connected", conn.remote);
            while (conn.open) {
                byte[] frame = conn.readMessage();
                if (frame == null) break;
                Envelope env = parseEnvelope(frame);
                if (env.type.isEmpty()) continue;
                hub.handle(env.type, env.payload, conn);
            }
        } catch (Exception ignored) {
            // Socket errors are routine: the peer simply went away.
        } finally {
            if (conn != null) {
                conn.open = false;
                try {
                    hub.handleDisconnect(conn);
                } catch (Exception ignored) {
                }
                conn.close();
                log("disconnected", conn.remote);
            } else {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            printUsage();
            return;
        }

        int port = DEFAULT_PORT;
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) {
            try {
                port = Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        Hub hub = new Hub();
        hub.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    hub.reap();
                } catch (Exception ignored) {
                }
            }
        }, EMPTY_ROOM_TTL_MS, EMPTY_ROOM_TTL_MS);

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
            log("listening", "0.0.0.0:" + port + "  (ws://<host>:" + port + "/ws)");
            while (true) {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().name("metroserver-conn").start(() -> serve(socket, hub));
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                MetroServer - self-hosted Listen Together backend

                Usage:  java MetroServer.java [port]
                        PORT=8080 java MetroServer.java

                App endpoint:   ws://<host>:<port>/ws
                Status check:   http://<host>:<port>/health

                Put a TLS terminator (nginx, Caddy, Cloudflare) in front and use
                wss:// in the app - release Android builds refuse cleartext.
                """);
    }

    private MetroServer() {
    }
}
