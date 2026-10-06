package com.jay.glossy.listentogether

import com.jay.glossy.R

import com.google.protobuf.ByteString
<<<<<<< HEAD
import com.google.protobuf.MessageLite
import com.metrolist.music.listentogether.proto.Listentogether
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
=======
import com.jay.glossy.listentogether.proto.Listentogether
import org.junit.Assert.assertEquals
>>>>>>> origin/main
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCodecTest {
    private val codec = MessageCodec(compressionEnabled = true)

<<<<<<< HEAD
    private fun envelope(
        type: String,
        payload: MessageLite,
    ): ByteArray =
        Listentogether.Envelope
            .newBuilder()
            .setType(type)
            .setPayload(ByteString.copyFrom(payload.toByteArray()))
            .build()
            .toByteArray()

=======
>>>>>>> origin/main
    @Test
    fun `playback timing fields survive a protobuf round trip`() {
        val action =
            PlaybackActionPayload(
                action = PlaybackActions.PLAY,
                trackId = "track",
                position = 1_234L,
                serverTime = 9_000L,
                revision = 12L,
                capturedAtServerTime = 8_950L,
            )

        val (type, payload) = codec.decode(codec.encode(MessageTypes.PLAYBACK_ACTION, action))
        val decoded = codec.decodePayload(MessageTypes.SYNC_PLAYBACK, payload) as PlaybackActionPayload

        assertEquals(MessageTypes.PLAYBACK_ACTION, type)
        assertEquals(action.action, decoded.action)
        assertEquals(action.trackId, decoded.trackId)
        assertEquals(action.position, decoded.position)
        assertEquals(action.serverTime, decoded.serverTime)
        assertEquals(action.revision, decoded.revision)
        assertEquals(action.capturedAtServerTime, decoded.capturedAtServerTime)
    }

    @Test
    fun `timestamped ping is encoded and pong is decoded`() {
        val ping = PingPayload(clientTime = 1_000L, sequence = 3L)
        val (_, pingBytes) = codec.decode(codec.encode(MessageTypes.PING, ping))
        val encodedPing = Listentogether.PingPayload.parseFrom(pingBytes)
        assertEquals(1_000L, encodedPing.clientTime)
        assertEquals(3L, encodedPing.sequence)

        val pong =
            Listentogether.PongPayload
                .newBuilder()
                .setClientTime(1_000L)
                .setServerReceiveTime(10_000L)
                .setServerSendTime(10_001L)
                .setSequence(3L)
                .build()
        val envelope =
            Listentogether.Envelope
                .newBuilder()
                .setType(MessageTypes.PONG)
                .setPayload(ByteString.copyFrom(pong.toByteArray()))
                .build()
        val (type, pongBytes) = codec.decode(envelope.toByteArray())
        val decoded = codec.decodePayload(type, pongBytes) as PongPayload

        assertEquals(PongPayload(1_000L, 10_000L, 10_001L, 3L), decoded)
        assertTrue(decoded.serverSendTime >= decoded.serverReceiveTime)
    }
<<<<<<< HEAD

    @Test
    fun `sync state that omits the queue and volume reports them as absent`() {
        val state =
            Listentogether.SyncStatePayload
                .newBuilder()
                .setIsPlaying(true)
                .setPosition(5_000L)
                .setLastUpdate(9_000L)
                .setRevision(4L)
                .build()

        val decoded = decodeSyncState(state)

        // "The host didn't say" must not arrive as "the host said empty/zero":
        // that is what muted every guest and emptied their queue on a recovery
        // snapshot, because protobuf has no presence for a repeated field or a
        // bare float.
        assertNull(decoded.queue)
        assertNull(decoded.volume)
        assertEquals(4L, decoded.revision)
    }

    @Test
    fun `sync state that carries a queue and volume reports them`() {
        val state =
            Listentogether.SyncStatePayload
                .newBuilder()
                .setIsPlaying(true)
                .setPosition(5_000L)
                .setLastUpdate(9_000L)
                .setRevision(4L)
                .setVolume(0.8f)
                .addQueue(
                    Listentogether.TrackInfo
                        .newBuilder()
                        .setId("next")
                        .setTitle("Next")
                        .setArtist("Artist")
                        .build(),
                ).build()

        val decoded = decodeSyncState(state)

        assertEquals(0.8f, decoded.volume!!, 0.0001f)
        assertEquals(listOf("next"), decoded.queue?.map { it.id })
    }

    @Test
    fun `playback action that omits the queue reports it as absent`() {
        val action =
            Listentogether.PlaybackActionPayload
                .newBuilder()
                .setAction(PlaybackActions.CHANGE_TRACK)
                .setRevision(7L)
                .build()

        val payload = codec.decode(envelope(MessageTypes.SYNC_PLAYBACK, action)).second
        val decoded = codec.decodePayload(MessageTypes.SYNC_PLAYBACK, payload) as PlaybackActionPayload

        assertNull(decoded.queue)
    }

    private fun decodeSyncState(state: Listentogether.SyncStatePayload): SyncStatePayload {
        val payload = codec.decode(envelope(MessageTypes.SYNC_STATE, state)).second
        return codec.decodePayload(MessageTypes.SYNC_STATE, payload) as SyncStatePayload
    }
=======
>>>>>>> origin/main
}
