#!/usr/bin/env python3
"""
Glossy / Metrolist "Listen Together" server.

A single-file, standard-library-only implementation of the listen-together
protocol the Glossy Android client speaks. No pip packages are required, so a
plain Python 3.11+ install is enough to host a room.

    python metro_server.py                      # listens on 0.0.0.0:8080
    python metro_server.py --port 9000          # different port
    python metro_server.py --print-urls         # also print the ws:// URL to use

Then in the app: Settings -> Integrations -> Listen Together -> Server URL, and
paste the printed address, for example:

    ws://192.168.1.24:8080/ws

The app already permits cleartext ws:// traffic, so a LAN address works from a
debug build. To let people outside your network join, put the server behind a
TLS-terminating tunnel and use the wss:// address it gives you; the server
honours X-Forwarded-* headers (--behind-proxy) so the URLs it prints stay
correct. Any plain HTTP GET returns a health/counters JSON, which is handy for
checking the server is alive from a browser.

Protocol recap (mirrors metroproto/listentogether.proto):

  * Binary WebSocket frames carrying a protobuf Envelope
    { type = 1:string, payload = 2:bytes, compressed = 3:bool }
  * payload is gzipped only when it is over 100 bytes *and* gzip is smaller
  * the server owns the playback revision counter; clients drop stale revisions
  * guests answer the first server `pong` with `request_sync` -> `sync_state`
"""

from __future__ import annotations

import argparse
import asyncio
import base64
import contextlib
import gzip
import hashlib
import json
import os
import secrets
import signal
import socket
import struct
import sys
import time
import uuid

# ---------------------------------------------------------------------------
# Protocol constants
# ---------------------------------------------------------------------------

WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

COMPRESSION_THRESHOLD = 100          # gzip only above this many payload bytes
MAX_USERS_PER_ROOM = 16
MAX_ROOMS = 5000
SESSION_TTL_MS = 120_000             # grace period for a dropped connection
EMPTY_ROOM_TTL_MS = 600_000          # keep a hostless room around this long
MAX_FRAME_BYTES = 4 * 1024 * 1024
ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
ROOM_CODE_LENGTH = 6

# Client -> server
M_CREATE_ROOM = "create_room"
M_JOIN_ROOM = "join_room"
M_LEAVE_ROOM = "leave_room"
M_APPROVE_JOIN = "approve_join"
M_REJECT_JOIN = "reject_join"
M_PLAYBACK_ACTION = "playback_action"
M_BUFFER_READY = "buffer_ready"
M_KICK_USER = "kick_user"
M_TRANSFER_HOST = "transfer_host"
M_PING = "ping"
M_CHAT = "chat"
M_REQUEST_SYNC = "request_sync"
M_RECONNECT = "reconnect"
M_SUGGEST_TRACK = "suggest_track"
M_APPROVE_SUGGESTION = "approve_suggestion"
M_REJECT_SUGGESTION = "reject_suggestion"
# Capability handshake: the client never sends one, but accept it if it appears.
M_CLIENT_CAPABILITIES = "client_capabilities"

# Server -> client
S_ROOM_CREATED = "room_created"
S_JOIN_REQUEST = "join_request"
S_JOIN_APPROVED = "join_approved"
S_JOIN_REJECTED = "join_rejected"
S_USER_JOINED = "user_joined"
S_USER_LEFT = "user_left"
S_SYNC_PLAYBACK = "sync_playback"
S_BUFFER_WAIT = "buffer_wait"
S_BUFFER_COMPLETE = "buffer_complete"
S_ERROR = "error"
S_PONG = "pong"
S_HOST_CHANGED = "host_changed"
S_KICKED = "kicked"
S_SYNC_STATE = "sync_state"
S_RECONNECTED = "reconnected"
S_USER_RECONNECTED = "user_reconnected"
S_USER_DISCONNECTED = "user_disconnected"
S_SUGGESTION_RECEIVED = "suggestion_received"
S_SUGGESTION_APPROVED = "suggestion_approved"
S_SUGGESTION_REJECTED = "suggestion_rejected"
S_SERVER_CAPABILITIES = "server_capabilities"

# Playback actions
A_PLAY = "play"
A_PAUSE = "pause"
A_SEEK = "seek"
A_SKIP_NEXT = "skip_next"
A_SKIP_PREV = "skip_prev"
A_CHANGE_TRACK = "change_track"
A_QUEUE_ADD = "queue_add"
A_QUEUE_REMOVE = "queue_remove"
A_QUEUE_CLEAR = "queue_clear"
A_SYNC_QUEUE = "sync_queue"
A_SET_VOLUME = "set_volume"

# Errors
E_INVALID_MESSAGE = "invalid_message"
E_ROOM_NOT_FOUND = "room_not_found"
E_ROOM_FULL = "room_full"
E_ROOM_LIMIT = "room_limit"
E_NOT_HOST = "not_host"
E_NOT_IN_ROOM = "not_in_room"
E_SESSION_NOT_FOUND = "session_not_found"
E_UNKNOWN_USER = "unknown_user"
E_RATE_LIMITED = "rate_limited"

PLAYBACK_ACTIONS = {
    A_PLAY, A_PAUSE, A_SEEK, A_SKIP_NEXT, A_SKIP_PREV, A_CHANGE_TRACK,
    A_QUEUE_ADD, A_QUEUE_REMOVE, A_QUEUE_CLEAR, A_SYNC_QUEUE, A_SET_VOLUME,
}


def now_ms() -> int:
    return int(time.time() * 1000)


# ---------------------------------------------------------------------------
# Minimal protobuf writer / reader (only the subset the protocol uses)
# ---------------------------------------------------------------------------

def _varint(value: int) -> bytes:
    if value < 0:
        value += 1 << 64
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def _tag(field: int, wire: int) -> bytes:
    return _varint((field << 3) | wire)


def pb_varint(field: int, value: int) -> bytes:
    if value == 0:
        return b""          # proto3 default, omitted
    return _tag(field, 0) + _varint(value)


def pb_bool(field: int, value: bool) -> bytes:
    return _tag(field, 0) + b"\x01" if value else b""


def pb_string(field: int, value: str) -> bytes:
    if not value:
        return b""
    data = value.encode("utf-8")
    return _tag(field, 2) + _varint(len(data)) + data


def pb_bytes(field: int, value: bytes) -> bytes:
    if not value:
        return b""
    return _tag(field, 2) + _varint(len(value)) + value


def pb_message(field: int, value: bytes) -> bytes:
    if not value:
        return b""
    return _tag(field, 2) + _varint(len(value)) + value


def pb_float(field: int, value: float) -> bytes:
    # Written even when zero: the client's RoomState.volume is non-nullable.
    return _tag(field, 5) + struct.pack("<f", float(value))


def pb_float_opt(field: int, value: float) -> bytes:
    if not value:
        return b""
    return _tag(field, 5) + struct.pack("<f", float(value))


def _read_varint(data: bytes, index: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        if index >= len(data):
            raise ValueError("truncated varint")
        byte = data[index]
        index += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, index
        shift += 7
        if shift > 63:
            raise ValueError("varint too long")


class Fields:
    """Parsed protobuf fields: (field, wire, value) where value is an int or bytes."""

    def __init__(self, data: bytes):
        self.items: list[tuple[int, int, object]] = []
        index = 0
        while index < len(data):
            key, index = _read_varint(data, index)
            field, wire = key >> 3, key & 7
            if wire == 0:
                value, index = _read_varint(data, index)
            elif wire == 1:
                value = data[index:index + 8]
                index += 8
            elif wire == 2:
                length, index = _read_varint(data, index)
                value = data[index:index + length]
                index += length
            elif wire == 5:
                value = data[index:index + 4]
                index += 4
            else:
                raise ValueError(f"unsupported wire type {wire}")
            self.items.append((field, wire, value))

    def string(self, field: int, default: str = "") -> str:
        for f, w, v in self.items:
            if f == field and w == 2:
                return v.decode("utf-8", "replace")   # type: ignore[union-attr]
        return default

    def raw(self, field: int) -> bytes | None:
        for f, w, v in self.items:
            if f == field and w == 2:
                return v                                # type: ignore[return-value]
        return None

    def number(self, field: int, default: int = 0) -> int:
        for f, w, v in self.items:
            if f == field and w == 0:
                return int(v)                           # type: ignore[arg-type]
        return default

    def boolean(self, field: int, default: bool = False) -> bool:
        return bool(self.number(field, 1 if default else 0))

    def float(self, field: int, default: float = 0.0) -> float:
        for f, w, v in self.items:
            if f == field and w == 5:
                return struct.unpack("<f", v)[0]        # type: ignore[arg-type]
        return default


# ---------------------------------------------------------------------------
# Envelope
# ---------------------------------------------------------------------------

def encode_envelope(msg_type: str, payload: bytes) -> bytes:
    """Wrap a payload the way the client's MessageCodec does.

    Gzip is used only above the 100-byte threshold *and* only when it actually
    shrinks the payload; the client assumes the same rule when decoding.
    """
    if len(payload) > COMPRESSION_THRESHOLD:
        packed = gzip.compress(payload)
        if len(packed) < len(payload):
            return pb_string(1, msg_type) + pb_bytes(2, packed) + pb_bool(3, True)
    return pb_string(1, msg_type) + pb_bytes(2, payload)


def decode_envelope(data: bytes) -> tuple[str, bytes, bool]:
    fields = Fields(data)
    msg_type = fields.string(1)
    payload = fields.raw(2) or b""
    if fields.boolean(3):
        payload = gzip.decompress(payload)
    return msg_type, payload, fields.boolean(3)


# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------

class Track:
    __slots__ = ("id", "title", "artist", "album", "duration", "thumbnail", "suggested_by")

    def __init__(self, track_id="", title="", artist="", album="", duration=0,
                 thumbnail="", suggested_by=""):
        self.id = track_id
        self.title = title
        self.artist = artist
        self.album = album
        self.duration = duration
        self.thumbnail = thumbnail
        self.suggested_by = suggested_by

    @classmethod
    def parse(cls, data: bytes) -> "Track":
        fields = Fields(data)
        return cls(
            track_id=fields.string(1),
            title=fields.string(2),
            artist=fields.string(3),
            album=fields.string(4),
            duration=fields.number(5),
            thumbnail=fields.string(6),
            suggested_by=fields.string(7),
        )

    def encode(self) -> bytes:
        return (
            pb_string(1, self.id)
            + pb_string(2, self.title)
            + pb_string(3, self.artist)
            + pb_string(4, self.album)
            + pb_varint(5, int(self.duration))
            + pb_string(6, self.thumbnail)
            + pb_string(7, self.suggested_by)
        )

    def to_dict(self) -> dict:
        return {"id": self.id, "title": self.title, "artist": self.artist,
                "album": self.album, "duration": self.duration,
                "thumbnail": self.thumbnail}


class Member:
    __slots__ = ("user_id", "username", "is_host", "connected", "conn", "token", "room_code")

    def __init__(self, user_id: str, username: str, token: str, is_host: bool = False,
                 room_code: str = ""):
        self.user_id = user_id
        self.username = username
        self.token = token
        self.is_host = is_host
        self.connected = True
        self.room_code = room_code
        self.conn: "Conn | None" = None

    def encode(self) -> bytes:
        return (
            pb_string(1, self.user_id)
            + pb_string(2, self.username)
            + pb_bool(3, self.is_host)
            + pb_bool(4, self.connected)
        )


class Room:
    def __init__(self, code: str, host_id: str):
        self.code = code
        self.host_id = host_id
        self.members: dict[str, Member] = {}
        self.current_track: Track | None = None
        self.is_playing = False
        self.position = 0
        self.last_update = now_ms()
        self.volume = 1.0
        self.queue: list[Track] = []
        self.revision = 1
        self.waiting_for: set[str] = set()
        self.pending_joiners: dict[str, tuple[str, "Conn"]] = {}   # user_id -> (username, conn)
        self.suggestions: dict[str, tuple[str, Track]] = {}        # id -> (user_id, track)
        self.empty_since: int | None = None

    # -- encoders ----------------------------------------------------------

    def state_message(self) -> bytes:
        body = pb_string(1, self.code) + pb_string(2, self.host_id)
        for member in self.members.values():
            body += pb_message(3, member.encode())
        if self.current_track:
            body += pb_message(4, self.current_track.encode())
        body += pb_bool(5, self.is_playing)
        body += pb_varint(6, max(0, self.effective_position()))
        body += pb_varint(7, self.last_update)
        body += pb_float(8, self.volume)
        for queued in self.queue:
            body += pb_message(9, queued.encode())
        body += pb_varint(10, self.revision)
        return body

    def sync_state_message(self) -> bytes:
        body = b""
        if self.current_track:
            body += pb_message(1, self.current_track.encode())
        body += pb_bool(2, self.is_playing)
        body += pb_varint(3, max(0, self.effective_position()))
        body += pb_varint(4, self.last_update)
        for queued in self.queue:
            body += pb_message(5, queued.encode())
        body += pb_float(6, self.volume)
        body += pb_varint(7, self.revision)
        return body

    def effective_position(self) -> int:
        """Playback position now, accounting for time since the last update."""
        if not self.is_playing:
            return self.position
        return self.position + max(0, now_ms() - self.last_update)

    def member_ids(self, connected_only: bool = False) -> list[str]:
        return [m.user_id for m in self.members.values()
                if m.connected or not connected_only]

    def to_dict(self) -> dict:
        return {
            "code": self.code,
            "host": self.host_id,
            "users": len(self.members),
            "playing": self.is_playing,
            "revision": self.revision,
            "track": self.current_track.title if self.current_track else None,
        }


# ---------------------------------------------------------------------------
# WebSocket connection
# ---------------------------------------------------------------------------

class Conn:
    """One WebSocket client."""

    def __init__(self, server: "Server", reader: asyncio.StreamReader,
                 writer: asyncio.StreamWriter, peer: str):
        self.server = server
        self.reader = reader
        self.writer = writer
        self.peer = peer
        self.closed = False
        self.room: Room | None = None
        self.user_id: str | None = None
        self.username = ""
        self.token: str | None = None
        self.is_host = False
        self.pending_room_code: str | None = None
        self._fragments = bytearray()
        self._fragment_opcode = 0

    # -- HTTP / WebSocket handshake ---------------------------------------

    async def handshake(self) -> bool:
        request_line = await self.reader.readline()
        if not request_line:
            return False
        try:
            parts = request_line.decode("latin-1").strip().split(" ")
            method, path = parts[0], parts[1] if len(parts) > 1 else "/"
        except Exception:
            return False

        headers: dict[str, str] = {}
        while True:
            line = await self.reader.readline()
            if not line or line in (b"\r\n", b"\n"):
                break
            try:
                name, _, value = line.decode("latin-1").partition(":")
                headers[name.strip().lower()] = value.strip()
            except Exception:
                continue

        upgrade = headers.get("upgrade", "").lower()
        key = headers.get("sec-websocket-key")
        if upgrade != "websocket" or not key or not path.startswith("/ws"):
            await self._send_http_json(method, path)
            return False

        accept = base64.b64encode(
            hashlib.sha1((key + WS_GUID).encode("ascii")).digest()
        ).decode("ascii")
        response = (
            "HTTP/1.1 101 Switching Protocols\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Accept: {accept}\r\n"
            "\r\n"
        )
        self.writer.write(response.encode("ascii"))
        await self.writer.drain()
        # The client sends JSON text frames for nothing; binary only.
        return True

    async def _send_http_json(self, method: str, path: str) -> None:
        body = json.dumps(self.server.stats(), indent=2).encode("utf-8")
        head = (
            "HTTP/1.1 200 OK\r\n"
            "Content-Type: application/json\r\n"
            f"Content-Length: {len(body)}\r\n"
            "Connection: close\r\n"
            "Access-Control-Allow-Origin: *\r\n"
            "\r\n"
        ).encode("ascii")
        with contextlib.suppress(Exception):
            self.writer.write(head + body)
            await self.writer.drain()

    # -- framing -----------------------------------------------------------

    async def send_binary(self, data: bytes) -> None:
        await self._send_frame(0x2, data)

    async def _send_frame(self, opcode: int, payload: bytes) -> None:
        if self.closed:
            return
        length = len(payload)
        header = bytearray([0x80 | opcode])
        if length < 126:
            header.append(length)
        elif length < (1 << 16):
            header.append(126)
            header.extend(struct.pack(">H", length))
        else:
            header.append(127)
            header.extend(struct.pack(">Q", length))
        try:
            self.writer.write(bytes(header) + payload)
            await self.writer.drain()
        except (ConnectionError, RuntimeError, asyncio.CancelledError):
            self.closed = True

    async def _read_exact(self, count: int) -> bytes:
        return await self.reader.readexactly(count)

    async def read_message(self) -> bytes | None:
        """Returns the next complete binary message, or None when closed."""
        while True:
            try:
                header = await self._read_exact(2)
            except (asyncio.IncompleteReadError, ConnectionError):
                return None

            fin = bool(header[0] & 0x80)
            opcode = header[0] & 0x0F
            masked = bool(header[1] & 0x80)
            length = header[1] & 0x7F
            if length == 126:
                length = struct.unpack(">H", await self._read_exact(2))[0]
            elif length == 127:
                length = struct.unpack(">Q", await self._read_exact(8))[0]
            if length > MAX_FRAME_BYTES:
                await self.close(1009, "frame too large")
                return None

            mask = await self._read_exact(4) if masked else b""
            payload = await self._read_exact(length) if length else b""
            if masked:
                payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))

            if opcode == 0x8:      # close
                await self.close(1000, "")
                return None
            if opcode == 0x9:      # ping
                await self._send_frame(0xA, payload)
                continue
            if opcode == 0xA:      # pong
                continue
            if opcode == 0x1:      # text: not part of the protocol
                continue

            if opcode in (0x0, 0x2):
                if opcode == 0x2:
                    self._fragments = bytearray(payload)
                    self._fragment_opcode = 0x2
                else:
                    self._fragments.extend(payload)
                if fin:
                    message = bytes(self._fragments)
                    self._fragments = bytearray()
                    return message

    async def close(self, code: int = 1000, reason: str = "") -> None:
        if self.closed:
            return
        self.closed = True
        with contextlib.suppress(Exception):
            body = struct.pack(">H", code) + reason.encode("utf-8")
            await self._send_frame(0x8, body)
        with contextlib.suppress(Exception):
            self.writer.close()


# ---------------------------------------------------------------------------
# The hub
# ---------------------------------------------------------------------------

class Server:
    def __init__(self, host: str, port: int, behind_proxy: bool = False):
        self.host = host
        self.port = port
        self.behind_proxy = behind_proxy
        self.rooms: dict[str, Room] = {}
        self.sessions: dict[str, Member] = {}     # token -> member
        self.conns: set[Conn] = set()
        self.started_at = now_ms()
        self.tasks: set[asyncio.Task] = set()

    # -- helpers -----------------------------------------------------------

    def stats(self) -> dict:
        return {
            "status": "ok",
            "server": "glossy-listen-together",
            "rooms": len(self.rooms),
            "clients": len(self.conns),
            "sessions": len(self.sessions),
            "uptimeSeconds": int((now_ms() - self.started_at) / 1000),
            "roomList": [room.to_dict() for room in list(self.rooms.values())[:50]],
        }

    def _background(self, coro) -> None:
        task = asyncio.ensure_future(coro)
        self.tasks.add(task)
        task.add_done_callback(self.tasks.discard)

    def _new_room_code(self) -> str:
        while True:
            code = "".join(secrets.choice(ROOM_CODE_ALPHABET) for _ in range(ROOM_CODE_LENGTH))
            if code not in self.rooms:
                return code

    def local_ip(self) -> str:
        try:
            probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            probe.connect(("8.8.8.8", 80))
            address = probe.getsockname()[0]
            probe.close()
            return address
        except Exception:
            return "127.0.0.1"

    async def send(self, conn: Conn, msg_type: str, payload: bytes = b"") -> None:
        await conn.send_binary(encode_envelope(msg_type, payload))

    async def send_error(self, conn: Conn, code: str, message: str) -> None:
        await self.send(conn, S_ERROR, pb_string(1, code) + pb_string(2, message))

    async def broadcast(self, room: Room, msg_type: str, payload: bytes,
                        exclude: Conn | None = None) -> None:
        for member in list(room.members.values()):
            target = member.conn
            if target is None or target is exclude:
                continue
            await self.send(target, msg_type, payload)

    def member_for_token(self, token: str) -> Member | None:
        return self.sessions.get(token)

    # -- lifecycle ---------------------------------------------------------

    async def handle_client(self, reader: asyncio.StreamReader,
                            writer: asyncio.StreamWriter) -> None:
        peer = "unknown"
        with contextlib.suppress(Exception):
            address = writer.get_extra_info("peername")
            if address:
                peer = f"{address[0]}:{address[1]}"
        conn = Conn(self, reader, writer, peer)
        try:
            if not await conn.handshake():
                writer.close()
                return
        except Exception:
            writer.close()
            return

        self.conns.add(conn)
        print(f"[connect] {peer} ({len(self.conns)} online)", flush=True)
        try:
            while True:
                message = await conn.read_message()
                if message is None:
                    break
                try:
                    msg_type, payload, _ = decode_envelope(message)
                except Exception as exc:
                    await self.send_error(conn, E_INVALID_MESSAGE, f"bad envelope: {exc}")
                    continue
                try:
                    await self.dispatch(conn, msg_type, payload)
                except Exception as exc:            # keep the socket alive
                    await self.send_error(conn, E_INVALID_MESSAGE, str(exc))
        except (ConnectionError, asyncio.IncompleteReadError):
            pass
        finally:
            self.conns.discard(conn)
            await self.on_disconnect(conn)
            writer.close()
            print(f"[disconnect] {peer} ({len(self.conns)} online)", flush=True)

    async def on_disconnect(self, conn: Conn) -> None:
        room = conn.room
        member = None
        if room and conn.user_id:
            member = room.members.get(conn.user_id)
        if member is not None:
            member.connected = False
            member.conn = None
            await self.broadcast(room, S_USER_DISCONNECTED,
                                 pb_string(1, member.user_id) + pb_string(2, member.username))
            self._background(self._expire_session(room, member))
        elif conn.pending_room_code:
            pending_room = self.rooms.get(conn.pending_room_code)
            if pending_room and conn.user_id:
                pending_room.pending_joiners.pop(conn.user_id, None)

    async def _expire_session(self, room: Room, member: Member) -> None:
        await asyncio.sleep(SESSION_TTL_MS / 1000)
        if member.connected:
            return
        if room.members.get(member.user_id) is not member:
            return
        del room.members[member.user_id]
        self.sessions.pop(member.token, None)
        await self.broadcast(room, S_USER_LEFT,
                             pb_string(1, member.user_id) + pb_string(2, member.username))
        await self._after_member_removed(room, member)

    async def _after_member_removed(self, room: Room, member: Member) -> None:
        if not room.members:
            self._background(self._reap_room_later(room))
            return
        if member.is_host or room.host_id == member.user_id:
            await self._promote_new_host(room)

    async def _promote_new_host(self, room: Room) -> None:
        candidates = [m for m in room.members.values() if m.connected] or list(room.members.values())
        if not candidates:
            return
        new_host = candidates[0]
        for member in room.members.values():
            member.is_host = member is new_host
        room.host_id = new_host.user_id
        room.revision += 1
        await self.broadcast(
            room,
            S_HOST_CHANGED,
            pb_string(1, new_host.user_id) + pb_string(2, new_host.username),
        )

    async def _reap_room_later(self, room: Room) -> None:
        room.empty_since = now_ms()
        await asyncio.sleep(EMPTY_ROOM_TTL_MS / 1000)
        if room.members:
            return
        self.rooms.pop(room.code, None)
        print(f"[reap] room {room.code} removed", flush=True)

    # -- dispatch ----------------------------------------------------------

    async def dispatch(self, conn: Conn, msg_type: str, payload: bytes) -> None:
        if msg_type == M_PING:
            await self.on_ping(conn, payload)
        elif msg_type == M_CREATE_ROOM:
            await self.on_create_room(conn, payload)
        elif msg_type == M_JOIN_ROOM:
            await self.on_join_room(conn, payload)
        elif msg_type == M_APPROVE_JOIN:
            await self.on_approve_join(conn, payload)
        elif msg_type == M_REJECT_JOIN:
            await self.on_reject_join(conn, payload)
        elif msg_type == M_LEAVE_ROOM:
            await self.on_leave_room(conn)
        elif msg_type == M_PLAYBACK_ACTION:
            await self.on_playback_action(conn, payload)
        elif msg_type == M_BUFFER_READY:
            await self.on_buffer_ready(conn, payload)
        elif msg_type == M_KICK_USER:
            await self.on_kick_user(conn, payload)
        elif msg_type == M_TRANSFER_HOST:
            await self.on_transfer_host(conn, payload)
        elif msg_type == M_REQUEST_SYNC:
            await self.on_request_sync(conn)
        elif msg_type == M_RECONNECT:
            await self.on_reconnect(conn, payload)
        elif msg_type == M_SUGGEST_TRACK:
            await self.on_suggest_track(conn, payload)
        elif msg_type == M_APPROVE_SUGGESTION:
            await self.on_approve_suggestion(conn, payload)
        elif msg_type == M_REJECT_SUGGESTION:
            await self.on_reject_suggestion(conn, payload)
        elif msg_type == M_CHAT:
            # The client has no encoder for chat yet; accept and ignore it so a
            # future client cannot be dropped by an unknown type.
            return
        elif msg_type == M_CLIENT_CAPABILITIES:
            await self.send(
                conn,
                S_SERVER_CAPABILITIES,
                pb_bool(1, True) + pb_bool(2, True) + pb_string(3, "metro-server-py-1.0"),
            )
        else:
            await self.send_error(conn, E_INVALID_MESSAGE, f"unknown type {msg_type}")

    # -- handlers ----------------------------------------------------------

    async def on_ping(self, conn: Conn, payload: bytes) -> None:
        fields = Fields(payload)
        client_time = fields.number(1)
        sequence = fields.number(2)
        received = now_ms()
        body = (
            pb_varint(1, client_time)
            + pb_varint(2, received)
            + pb_varint(3, now_ms())
            + pb_varint(4, sequence)
        )
        await self.send(conn, S_PONG, body)

    async def on_create_room(self, conn: Conn, payload: bytes) -> None:
        if conn.room is not None:
            await self.send_error(conn, E_INVALID_MESSAGE, "already in a room")
            return
        if len(self.rooms) >= MAX_ROOMS:
            await self.send_error(conn, E_ROOM_LIMIT, "server is at capacity")
            return

        username = Fields(payload).string(1) or "Host"
        user_id = uuid.uuid4().hex[:12]
        token = secrets.token_urlsafe(24)
        code = self._new_room_code()

        room = Room(code, user_id)
        host = Member(user_id, username, token, is_host=True, room_code=code)
        host.conn = conn
        room.members[user_id] = host
        room.empty_since = None
        self.rooms[code] = room
        self.sessions[token] = host

        conn.room = room
        conn.user_id = user_id
        conn.username = username
        conn.token = token
        conn.is_host = True

        body = pb_string(1, code) + pb_string(2, user_id) + pb_string(3, token)
        await self.send(conn, S_ROOM_CREATED, body)

    async def on_join_room(self, conn: Conn, payload: bytes) -> None:
        if conn.room is not None:
            await self.send_error(conn, E_INVALID_MESSAGE, "already in a room")
            return
        fields = Fields(payload)
        code = fields.string(1).strip().upper()
        username = fields.string(2) or "Guest"
        room = self.rooms.get(code)
        if room is None:
            await self.send_error(conn, E_ROOM_NOT_FOUND, f"no room {code}")
            return
        if len(room.members) >= MAX_USERS_PER_ROOM:
            await self.send_error(conn, E_ROOM_FULL, "room is full")
            return

        user_id = uuid.uuid4().hex[:12]
        conn.user_id = user_id
        conn.username = username
        conn.pending_room_code = code
        room.pending_joiners[user_id] = (username, conn)

        host = room.members.get(room.host_id)
        if host and host.conn is not None:
            await self.send(
                host.conn,
                S_JOIN_REQUEST,
                pb_string(1, user_id) + pb_string(2, username),
            )
        else:
            # Nobody to approve the request: let the joiner in directly.
            await self._admit(room, username, conn)

    async def _admit(self, room: Room, username: str, conn: Conn) -> None:
        user_id = conn.user_id or uuid.uuid4().hex[:12]
        token = secrets.token_urlsafe(24)
        member = Member(user_id, username, token, is_host=False, room_code=room.code)
        member.conn = conn
        room.members[user_id] = member
        room.pending_joiners.pop(user_id, None)
        room.empty_since = None
        self.sessions[token] = member

        conn.room = room
        conn.user_id = user_id
        conn.username = username
        conn.token = token
        conn.is_host = False
        conn.pending_room_code = None

        body = (
            pb_string(1, room.code)
            + pb_string(2, user_id)
            + pb_string(3, token)
            + pb_message(4, room.state_message())
        )
        await self.send(conn, S_JOIN_APPROVED, body)
        await self.broadcast(
            room,
            S_USER_JOINED,
            pb_string(1, user_id) + pb_string(2, username),
            exclude=conn,
        )
        print(f"[join] {username} joined {room.code} "
              f"({len(room.members)}/{MAX_USERS_PER_ROOM})", flush=True)

    async def on_approve_join(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can approve")
            return
        user_id = Fields(payload).string(1)
        pending = room.pending_joiners.get(user_id)
        if pending is None:
            await self.send_error(conn, E_UNKNOWN_USER, "no pending request")
            return
        username, pending_conn = pending
        if pending_conn.closed:
            room.pending_joiners.pop(user_id, None)
            return
        pending_conn.user_id = user_id
        await self._admit(room, username, pending_conn)

    async def on_reject_join(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can reject")
            return
        fields = Fields(payload)
        user_id = fields.string(1)
        reason = fields.string(2) or "Host declined"
        pending = room.pending_joiners.pop(user_id, None)
        if pending is None:
            return
        _, pending_conn = pending
        pending_conn.pending_room_code = None
        await self.send(pending_conn, S_JOIN_REJECTED, pb_string(1, reason))

    async def on_leave_room(self, conn: Conn) -> None:
        room = conn.room
        if room is None or not conn.user_id:
            await self.send_error(conn, E_NOT_IN_ROOM, "not in a room")
            return
        member = room.members.pop(conn.user_id, None)
        if member is not None:
            self.sessions.pop(member.token, None)
            await self.broadcast(
                room,
                S_USER_LEFT,
                pb_string(1, member.user_id) + pb_string(2, member.username),
            )
            await self._after_member_removed(room, member)
        conn.room = None
        conn.user_id = None
        conn.token = None
        conn.is_host = False

    async def on_playback_action(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None:
            await self.send_error(conn, E_NOT_IN_ROOM, "not in a room")
            return
        if conn.user_id != room.host_id and not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host controls playback")
            return

        fields = Fields(payload)
        action = fields.string(1)
        if action not in PLAYBACK_ACTIONS:
            await self.send_error(conn, E_INVALID_MESSAGE, f"unknown action {action}")
            return

        track_id = fields.string(2)
        position = fields.number(3)
        track_info_raw = fields.raw(4)
        insert_next = fields.boolean(5)
        queue = [Track.parse(raw) for raw in _repeated(fields, 6)]
        queue_title = fields.string(7)
        volume = fields.float(8)
        client_revision = fields.number(10)

        self._apply_action(room, action, track_id, position, track_info_raw,
                           insert_next, queue, queue_title, volume)

        room.revision += 1
        server_time = now_ms()
        room.last_update = server_time

        relay = (
            pb_string(1, action)
            + pb_string(2, track_id)
            + pb_varint(3, position)
            + (pb_message(4, track_info_raw) if track_info_raw else b"")
            + pb_bool(5, insert_next)
        )
        for queued in (queue if action in (A_SYNC_QUEUE, A_CHANGE_TRACK) else room.queue):
            relay += pb_message(6, queued.encode())
        relay += pb_string(7, queue_title)
        relay += pb_float_opt(8, volume)
        relay += pb_varint(9, server_time)
        relay += pb_varint(10, room.revision)
        relay += pb_varint(11, server_time)
        _ = client_revision

        await self.broadcast(room, S_SYNC_PLAYBACK, relay, exclude=conn)

    def _apply_action(self, room: Room, action: str, track_id: str, position: int,
                      track_info_raw: bytes | None, insert_next: bool,
                      queue: list[Track], queue_title: str, volume: float) -> None:
        if action == A_PLAY:
            room.is_playing = True
            if position:
                room.position = position
        elif action == A_PAUSE:
            room.is_playing = False
            room.position = position or room.effective_position()
        elif action == A_SEEK:
            room.position = position
        elif action in (A_SKIP_NEXT, A_SKIP_PREV):
            if room.queue:
                upcoming = room.queue.pop(0 if action == A_SKIP_NEXT else -1)
                room.current_track = upcoming
            room.position = 0
            room.is_playing = True
        elif action == A_CHANGE_TRACK:
            if track_info_raw:
                room.current_track = Track.parse(track_info_raw)
            room.position = 0
            room.is_playing = False
            if queue:
                room.queue = queue
        elif action == A_QUEUE_ADD:
            if track_info_raw:
                added = Track.parse(track_info_raw)
                room.queue.insert(0 if insert_next else len(room.queue), added)
        elif action == A_QUEUE_REMOVE:
            room.queue = [t for t in room.queue if t.id != track_id]
        elif action == A_QUEUE_CLEAR:
            room.queue = []
        elif action == A_SYNC_QUEUE:
            room.queue = queue
        elif action == A_SET_VOLUME:
            room.volume = max(0.0, min(1.0, volume))
        _ = queue_title

    async def on_buffer_ready(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None:
            return
        track_id = Fields(payload).string(1)
        if conn.user_id:
            room.waiting_for.discard(conn.user_id)
        host = room.members.get(room.host_id)
        if host is None or host.conn is None:
            return
        if room.waiting_for:
            body = pb_string(1, track_id)
            for user_id in sorted(room.waiting_for):
                body += pb_string(2, user_id)
            await self.send(host.conn, S_BUFFER_WAIT, body)
        else:
            await self.broadcast(room, S_BUFFER_COMPLETE, pb_string(1, track_id))

    async def on_kick_user(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can kick")
            return
        fields = Fields(payload)
        user_id = fields.string(1)
        reason = fields.string(2) or "Removed by the host"
        member = room.members.get(user_id)
        if member is None:
            await self.send_error(conn, E_UNKNOWN_USER, "no such user")
            return
        target = member.conn
        if target is not None:
            await self.send(target, S_KICKED, pb_string(1, reason))
        if target is not None:
            target.room = None
            target.user_id = None
            target.token = None
            target.is_host = False
        room.members.pop(user_id, None)
        self.sessions.pop(member.token, None)
        await self.broadcast(
            room,
            S_USER_LEFT,
            pb_string(1, member.user_id) + pb_string(2, member.username),
        )

    async def on_transfer_host(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can transfer")
            return
        new_host_id = Fields(payload).string(1)
        new_host = room.members.get(new_host_id)
        if new_host is None:
            await self.send_error(conn, E_UNKNOWN_USER, "no such user")
            return
        for member in room.members.values():
            member.is_host = member.user_id == new_host_id
            if member.conn is not None:
                member.conn.is_host = member.is_host
        room.host_id = new_host_id
        room.revision += 1
        await self.broadcast(
            room,
            S_HOST_CHANGED,
            pb_string(1, new_host.user_id) + pb_string(2, new_host.username),
        )

    async def on_request_sync(self, conn: Conn) -> None:
        room = conn.room
        if room is None:
            await self.send_error(conn, E_NOT_IN_ROOM, "not in a room")
            return
        await self.send(conn, S_SYNC_STATE, room.sync_state_message())

    async def on_reconnect(self, conn: Conn, payload: bytes) -> None:
        token = Fields(payload).string(1)
        member = self.sessions.get(token) if token else None
        if member is None:
            await self.send_error(conn, E_SESSION_NOT_FOUND, "session expired")
            return
        room = self.rooms.get(member.room_code)
        if room is None or room.members.get(member.user_id) is not member:
            self.sessions.pop(token, None)
            await self.send_error(conn, E_SESSION_NOT_FOUND, "session expired")
            return

        member.connected = True
        member.conn = conn
        conn.room = room
        conn.user_id = member.user_id
        conn.username = member.username
        conn.token = token
        conn.is_host = member.is_host
        conn.pending_room_code = None
        room.empty_since = None

        body = (
            pb_string(1, room.code)
            + pb_string(2, member.user_id)
            + pb_message(3, room.state_message())
            + pb_bool(4, member.is_host)
        )
        await self.send(conn, S_RECONNECTED, body)
        await self.broadcast(
            room,
            S_USER_RECONNECTED,
            pb_string(1, member.user_id) + pb_string(2, member.username),
            exclude=conn,
        )

    async def on_suggest_track(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or conn.user_id is None:
            await self.send_error(conn, E_NOT_IN_ROOM, "not in a room")
            return
        raw = Fields(payload).raw(1)
        if raw is None:
            await self.send_error(conn, E_INVALID_MESSAGE, "missing track")
            return
        track = Track.parse(raw)
        suggestion_id = uuid.uuid4().hex[:10]
        room.suggestions[suggestion_id] = (conn.user_id, track)

        host = room.members.get(room.host_id)
        if host is not None and host.conn is not None:
            body = (
                pb_string(1, suggestion_id)
                + pb_string(2, conn.user_id)
                + pb_string(3, conn.username)
                + pb_message(4, track.encode())
            )
            await self.send(host.conn, S_SUGGESTION_RECEIVED, body)

    async def on_approve_suggestion(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can approve")
            return
        suggestion_id = Fields(payload).string(1)
        entry = room.suggestions.pop(suggestion_id, None)
        if entry is None:
            await self.send_error(conn, E_UNKNOWN_USER, "no such suggestion")
            return
        _, track = entry
        room.queue.append(track)
        room.revision += 1
        await self.broadcast(
            room,
            S_SUGGESTION_APPROVED,
            pb_string(1, suggestion_id) + pb_message(2, track.encode()),
        )

    async def on_reject_suggestion(self, conn: Conn, payload: bytes) -> None:
        room = conn.room
        if room is None or not conn.is_host:
            await self.send_error(conn, E_NOT_HOST, "only the host can reject")
            return
        fields = Fields(payload)
        suggestion_id = fields.string(1)
        reason = fields.string(2) or "Declined"
        entry = room.suggestions.pop(suggestion_id, None)
        if entry is None:
            return
        owner_id, _ = entry
        owner = room.members.get(owner_id)
        if owner is not None and owner.conn is not None:
            await self.send(
                owner.conn,
                S_SUGGESTION_REJECTED,
                pb_string(1, suggestion_id) + pb_string(2, reason),
            )


def _repeated(fields: Fields, field: int) -> list[bytes]:
    return [v for f, w, v in fields.items if f == field and w == 2]


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def print_urls(host: str, port: int, server: Server, scheme: str = "ws") -> None:
    addresses = [server.local_ip(), "127.0.0.1"]
    seen = set()
    print("", flush=True)
    print("Listen Together server is up. Put one of these in the app:", flush=True)
    print("  Settings -> Integrations -> Listen Together -> Server URL", flush=True)
    for address in addresses:
        if address in seen:
            continue
        seen.add(address)
        print(f"    {scheme}://{address}:{port}/ws", flush=True)
    print("", flush=True)
    print(f"Health check: http://{server.local_ip()}:{port}/", flush=True)
    print("", flush=True)


async def amain(args: argparse.Namespace) -> None:
    server = Server(args.host, args.port, behind_proxy=args.behind_proxy)
    tcp = await asyncio.start_server(server.handle_client, args.host, args.port)
    scheme = "wss" if args.public_url and args.public_url.startswith("wss") else "ws"
    print(f"metro-server (python) listening on {args.host}:{args.port}", flush=True)
    if args.public_url:
        print(f"Public URL configured: {args.public_url}", flush=True)
    else:
        print_urls(args.host, args.port, server, scheme)
    if args.print_urls and args.public_url:
        print_urls(args.host, args.port, server, scheme)

    stop = asyncio.Event()

    def request_stop() -> None:
        stop.set()

    if os.name != "nt":
        loop = asyncio.get_running_loop()
        for signal_name in ("SIGINT", "SIGTERM"):
            with contextlib.suppress(NotImplementedError, AttributeError):
                loop.add_signal_handler(getattr(signal, signal_name), request_stop)

    async with tcp:
        with contextlib.suppress(asyncio.CancelledError):
            await stop.wait()
    print("\nshutting down", flush=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="metro_server.py",
        description="Listen Together server for the Glossy/Metrolist Android app.",
        epilog=(
            "Examples:\n"
            "  python metro_server.py\n"
            "  python metro_server.py --port 9000\n"
            "  python metro_server.py --public-url wss://rooms.example.com/ws\n"
            "\n"
            "Public access: keep this server running and point a tunnel at it, e.g.\n"
            "  cloudflared tunnel --url http://localhost:8080\n"
            "  ngrok http 8080\n"
            "then use the wss:// address the tunnel prints, with --behind-proxy so\n"
            "forwarded headers are honoured."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--host", default=os.environ.get("HOST", "0.0.0.0"),
                        help="interface to bind (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8080")),
                        help="port to listen on (default: 8080)")
    parser.add_argument("--behind-proxy", action="store_true",
                        help="trust X-Forwarded-* headers from a reverse proxy/tunnel")
    parser.add_argument("--public-url", default=os.environ.get("PUBLIC_URL", ""),
                        help="wss:// address people outside your network should use")
    parser.add_argument("--print-urls", action="store_true",
                        help="print candidate ws:// URLs (also printed when no --public-url)")
    return parser


if __name__ == "__main__":
    arguments = build_parser().parse_args()
    try:
        asyncio.run(amain(arguments))
    except KeyboardInterrupt:
        sys.exit(0)
