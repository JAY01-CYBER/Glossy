#!/usr/bin/env python3
"""
End-to-end smoke test for metro_server.py.

This deliberately re-implements the wire format (protobuf subset, gzip
envelope, RFC 6455 client framing) instead of importing the server's code, so a
pass here means the *bytes* are right and not merely that the server agrees with
itself.

    python metro_server.py --port 8099      # in one terminal
    python smoke_test.py --port 8099        # in another

It walks the real client's flow: create a room, request to join, approve,
answer the first pong with request_sync, drive a playback action, run the
buffer handshake, reconnect with the session token, then kick a user.
"""

from __future__ import annotations

import argparse
import asyncio
import base64
import gzip
import os
import secrets
import struct
import sys

# ---------------------------------------------------------------------------
# Independent protobuf subset
# ---------------------------------------------------------------------------

def varint(value: int) -> bytes:
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


def tag(field: int, wire: int) -> bytes:
    return varint((field << 3) | wire)


def pb_string(field: int, value: str) -> bytes:
    if not value:
        return b""
    data = value.encode()
    return tag(field, 2) + varint(len(data)) + data


def pb_bytes(field: int, value: bytes) -> bytes:
    if not value:
        return b""
    return tag(field, 2) + varint(len(value)) + value


def pb_message(field: int, value: bytes) -> bytes:
    return pb_bytes(field, value)


def pb_varint_field(field: int, value: int) -> bytes:
    if value == 0:
        return b""
    return tag(field, 0) + varint(value)


def pb_bool(field: int, value: bool) -> bytes:
    return tag(field, 0) + b"\x01" if value else b""


def pb_float(field: int, value: float) -> bytes:
    return tag(field, 5) + struct.pack("<f", value)


def read_fields(data: bytes) -> list[tuple[int, int, object]]:
    items: list[tuple[int, int, object]] = []
    index = 0
    while index < len(data):
        key = 0
        shift = 0
        while True:
            byte = data[index]
            index += 1
            key |= (byte & 0x7F) << shift
            if not byte & 0x80:
                break
            shift += 7
        field, wire = key >> 3, key & 7
        if wire == 0:
            value = 0
            shift = 0
            while True:
                byte = data[index]
                index += 1
                value |= (byte & 0x7F) << shift
                if not byte & 0x80:
                    break
                shift += 7
            items.append((field, wire, value))
        elif wire == 2:
            length = 0
            shift = 0
            while True:
                byte = data[index]
                index += 1
                length |= (byte & 0x7F) << shift
                if not byte & 0x80:
                    break
                shift += 7
            items.append((field, wire, data[index:index + length]))
            index += length
        elif wire == 5:
            items.append((field, wire, data[index:index + 4]))
            index += 4
        else:
            raise AssertionError(f"unexpected wire type {wire}")
    return items


def f_string(fields, number, default=""):
    for f, w, v in fields:
        if f == number and w == 2:
            return v.decode()
    return default


def f_number(fields, number, default=0):
    for f, w, v in fields:
        if f == number and w == 0:
            return int(v)
    return default


def f_float(fields, number, default=0.0):
    for f, w, v in fields:
        if f == number and w == 5:
            return struct.unpack("<f", v)[0]
    return default


def f_message(fields, number) -> bytes | None:
    for f, w, v in fields:
        if f == number and w == 2:
            return v
    return None


def encode_envelope(msg_type: str, payload: bytes) -> bytes:
    if len(payload) > 100:
        packed = gzip.compress(payload)
        if len(packed) < len(payload):
            return pb_string(1, msg_type) + pb_bytes(2, packed) + pb_bool(3, True)
    return pb_string(1, msg_type) + pb_bytes(2, payload)


def decode_envelope(data: bytes) -> tuple[str, bytes]:
    fields = read_fields(data)
    payload = f_message(fields, 2) or b""
    if f_number(fields, 3, 1 if any(f == 3 for f, _, _ in fields) else 0):
        payload = gzip.decompress(payload)
    return f_string(fields, 1), payload


# ---------------------------------------------------------------------------
# Independent WebSocket client
# ---------------------------------------------------------------------------

class WsClient:
    def __init__(self, name: str, host: str, port: int):
        self.name = name
        self.host = host
        self.port = port
        self.reader: asyncio.StreamReader | None = None
        self.writer: asyncio.StreamWriter | None = None
        self.inbox: asyncio.Queue[tuple[str, bytes]] = asyncio.Queue()
        self.raw_events: list[str] = []
        self._pump: asyncio.Task | None = None

    async def connect(self) -> None:
        self.reader, self.writer = await asyncio.open_connection(self.host, self.port)
        key = base64.b64encode(secrets.token_bytes(16)).decode()
        request = (
            f"GET /ws HTTP/1.1\r\n"
            f"Host: {self.host}:{self.port}\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Key: {key}\r\n"
            "Sec-WebSocket-Version: 13\r\n"
            "\r\n"
        )
        self.writer.write(request.encode())
        await self.writer.drain()

        status = await self.reader.readline()
        assert b"101" in status, f"{self.name}: handshake failed: {status!r}"
        headers = {}
        while True:
            line = await self.reader.readline()
            if line in (b"\r\n", b"\n", b""):
                break
            name, _, value = line.decode().partition(":")
            headers[name.strip().lower()] = value.strip()
        assert headers.get("upgrade", "").lower() == "websocket", f"{self.name}: no upgrade header"
        self._pump = asyncio.ensure_future(self._read_loop())

    async def send(self, msg_type: str, payload: bytes = b"") -> None:
        assert self.writer is not None
        data = encode_envelope(msg_type, payload)
        mask = secrets.token_bytes(4)
        header = bytearray([0x80 | 0x2])
        length = len(data)
        if length < 126:
            header.append(0x80 | length)
        elif length < (1 << 16):
            header.append(0x80 | 126)
            header.extend(struct.pack(">H", length))
        else:
            header.append(0x80 | 127)
            header.extend(struct.pack(">Q", length))
        masked = bytes(b ^ mask[i % 4] for i, b in enumerate(data))
        self.writer.write(bytes(header) + mask + masked)
        await self.writer.drain()

    async def _read_loop(self) -> None:
        assert self.reader is not None
        try:
            while True:
                header = await self.reader.readexactly(2)
                opcode = header[0] & 0x0F
                length = header[1] & 0x7F
                if length == 126:
                    length = struct.unpack(">H", await self.reader.readexactly(2))[0]
                elif length == 127:
                    length = struct.unpack(">Q", await self.reader.readexactly(8))[0]
                payload = await self.reader.readexactly(length) if length else b""
                if opcode == 0x8:
                    self.raw_events.append("close")
                    return
                if opcode == 0x9:
                    continue
                if opcode == 0x2:
                    msg_type, body = decode_envelope(payload)
                    await self.inbox.put((msg_type, body))
        except (asyncio.IncompleteReadError, ConnectionError):
            return

    async def expect(self, *wanted: str, timeout: float = 5.0) -> tuple[str, bytes]:
        """Waits for one of the given message types, ignoring the others."""
        deadline = asyncio.get_running_loop().time() + timeout
        while True:
            remaining = deadline - asyncio.get_running_loop().time()
            if remaining <= 0:
                raise AssertionError(f"{self.name}: timed out waiting for {wanted}")
            msg_type, body = await asyncio.wait_for(self.inbox.get(), timeout=remaining)
            if msg_type in wanted:
                return msg_type, body
            self.raw_events.append(msg_type)

    async def close(self) -> None:
        if self._pump:
            self._pump.cancel()
        if self.writer:
            self.writer.close()


# ---------------------------------------------------------------------------
# The test
# ---------------------------------------------------------------------------

passed: list[str] = []
failed: list[str] = []


def check(condition: bool, label: str) -> None:
    (passed if condition else failed).append(label)
    print(f"  {'PASS' if condition else 'FAIL'}  {label}", flush=True)


async def run(host: str, port: int) -> int:
    host_client = WsClient("host", host, port)
    guest_client = WsClient("guest", host, port)

    print("connecting two clients", flush=True)
    await host_client.connect()
    await guest_client.connect()

    # 1. create_room -------------------------------------------------------
    await host_client.send("create_room", pb_string(1, "Aman"))
    msg_type, body = await host_client.expect("room_created", "error")
    check(msg_type == "room_created", f"create_room answered with {msg_type}")
    fields = read_fields(body)
    room_code = f_string(fields, 1)
    host_id = f_string(fields, 2)
    host_token = f_string(fields, 3)
    check(len(room_code) == 6, f"room code looks valid ({room_code})")
    check(bool(host_id), "host user id returned")
    check(bool(host_token), "host session token returned")

    # 2. ping -> pong ------------------------------------------------------
    await host_client.send("ping", pb_varint_field(1, 123456789) + pb_varint_field(2, 7))
    msg_type, body = await host_client.expect("pong", "error")
    fields = read_fields(body)
    check(msg_type == "pong", f"ping answered with {msg_type}")
    check(f_number(fields, 1) == 123456789, "pong echoes client_time")
    check(f_number(fields, 2) > 0, "pong carries server_receive_time")
    check(f_number(fields, 3) >= f_number(fields, 2), "server_send_time >= receive time")
    check(f_number(fields, 4) == 7, "pong echoes the sequence")

    # 3. join_room -> join_request -> approve_join -------------------------
    await guest_client.send("join_room", pb_string(1, room_code) + pb_string(2, "Riya"))
    msg_type, body = await host_client.expect("join_request", "error")
    check(msg_type == "join_request", f"host notified with {msg_type}")
    fields = read_fields(body)
    guest_id = f_string(fields, 1)
    check(f_string(fields, 2) == "Riya", "join request carries the username")

    await host_client.send("approve_join", pb_string(1, guest_id))
    msg_type, body = await guest_client.expect("join_approved", "error")
    check(msg_type == "join_approved", f"guest admitted with {msg_type}")
    fields = read_fields(body)
    guest_token = f_string(fields, 3)
    check(f_string(fields, 1) == room_code, "join_approved carries the room code")
    state = f_message(fields, 4)
    check(state is not None, "join_approved carries the room state")
    if state is not None:
        state_fields = read_fields(state)
        check(f_string(state_fields, 1) == room_code, "state.room_code matches")
        check(f_string(state_fields, 2) == host_id, "state.host_id is the host")
        check(len([1 for f, w, _ in state_fields if f == 3 and w == 2]) == 2,
              "state lists both users")
        check(f_number(state_fields, 10) >= 1, "state carries a revision")
        check(any(f == 8 and w == 5 for f, w, _ in state_fields),
              "state carries volume as a float")
    await host_client.expect("user_joined")

    # 4. request_sync -> sync_state ---------------------------------------
    await guest_client.send("request_sync")
    msg_type, body = await guest_client.expect("sync_state", "error")
    check(msg_type == "sync_state", f"request_sync answered with {msg_type}")

    # 5. playback_action -> sync_playback ---------------------------------
    track = (
        pb_string(1, "track-1") + pb_string(2, "Moth To A Flame")
        + pb_string(3, "Swedish House Mafia") + pb_string(4, "Dawn FM")
        + pb_varint_field(5, 214000) + pb_string(6, "https://example.test/art.jpg")
    )
    await host_client.send(
        "playback_action",
        pb_string(1, "change_track") + pb_message(4, track) + pb_varint_field(10, 0),
    )
    msg_type, body = await guest_client.expect("sync_playback", "error")
    check(msg_type == "sync_playback", f"guest received {msg_type}")
    fields = read_fields(body)
    check(f_string(fields, 1) == "change_track", "relayed action matches")
    check(f_number(fields, 10) >= 2, "server assigned a higher revision")
    check(f_number(fields, 9) > 0, "relay carries server_time")
    relayed_track = f_message(fields, 4)
    if relayed_track is not None:
        check(f_string(read_fields(relayed_track), 2) == "Moth To A Flame",
              "track info round-tripped")

    await host_client.send("playback_action", pb_string(1, "play") + pb_varint_field(3, 1200))
    msg_type, body = await guest_client.expect("sync_playback")
    fields = read_fields(body)
    check(f_string(fields, 1) == "play" and f_number(fields, 3) == 1200,
          "play + position relayed")

    # 6. buffer handshake --------------------------------------------------
    await guest_client.send("buffer_ready", pb_string(1, "track-1"))
    msg_type, body = await host_client.expect("buffer_complete", "buffer_wait")
    check(msg_type == "buffer_complete", f"host told the buffer is ready ({msg_type})")

    # 7. queue_add then sync_state sees it ---------------------------------
    await host_client.send(
        "playback_action",
        pb_string(1, "queue_add") + pb_message(4, pb_string(1, "track-2") + pb_string(2, "Dracula")),
    )
    await guest_client.expect("sync_playback")
    await guest_client.send("request_sync")
    msg_type, body = await guest_client.expect("sync_state")
    fields = read_fields(body)
    queued = [v for f, w, v in fields if f == 5 and w == 2]
    check(len(queued) == 1, f"queue survived on the server ({len(queued)} entry)")

    # 8. host-only guard ---------------------------------------------------
    await guest_client.send("playback_action", pb_string(1, "pause"))
    msg_type, body = await guest_client.expect("error", "sync_playback")
    fields = read_fields(body)
    check(msg_type == "error" and f_string(fields, 1) == "not_host",
          f"guest cannot drive playback ({msg_type}/{f_string(fields, 1)})")

    # 9. reconnect with the session token ---------------------------------
    await guest_client.close()
    await asyncio.sleep(0.3)
    rejoin = WsClient("guest-reconnect", host, port)
    await rejoin.connect()
    await rejoin.send("reconnect", pb_string(1, guest_token))
    msg_type, body = await rejoin.expect("reconnected", "error")
    check(msg_type == "reconnected", f"session rejoined with {msg_type}")
    if msg_type == "reconnected":
        fields = read_fields(body)
        check(f_string(fields, 1) == room_code, "reconnected carries the room code")
        check(f_bool := f_number(fields, 4) == 0, "reconnected marks the guest as not host")
        del f_bool

    # 10. bad session ------------------------------------------------------
    stranger = WsClient("stranger", host, port)
    await stranger.connect()
    await stranger.send("reconnect", pb_string(1, "not-a-real-token"))
    msg_type, body = await stranger.expect("error")
    fields = read_fields(body)
    check(f_string(fields, 1) == "session_not_found",
          "unknown session is rejected with session_not_found")

    # 11. join a room that does not exist ---------------------------------
    await stranger.send("join_room", pb_string(1, "ZZZZZZ") + pb_string(2, "Nobody"))
    msg_type, body = await stranger.expect("error")
    fields = read_fields(body)
    check(f_string(fields, 1) == "room_not_found", "unknown room is rejected")

    # 12. kick -------------------------------------------------------------
    await host_client.send("kick_user", pb_string(1, guest_id) + pb_string(2, "Bye"))
    msg_type, body = await rejoin.expect("kicked", "user_left", "error")
    check(msg_type == "kicked", f"kicked user notified ({msg_type})")
    if msg_type == "kicked":
        check(f_string(read_fields(body), 1) == "Bye", "kick reason relayed")

    # 13. health endpoint --------------------------------------------------
    reader, writer = await asyncio.open_connection(host, port)
    writer.write(b"GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
    await writer.drain()
    raw = await reader.read()
    writer.close()
    check(b'"status": "ok"' in raw or b'"status":"ok"' in raw, "health endpoint answers ok")

    for client in (host_client, rejoin, stranger):
        await client.close()

    print("")
    print(f"{len(passed)} passed, {len(failed)} failed", flush=True)
    for label in failed:
        print(f"  failed: {label}", flush=True)
    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser(description="End-to-end test for metro_server.py")
    parser.add_argument("--host", default=os.environ.get("HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8080")))
    args = parser.parse_args()
    try:
        return asyncio.run(run(args.host, args.port))
    except AssertionError as exc:
        print(f"\nFAILED: {exc}", flush=True)
        return 1
    except (ConnectionRefusedError, OSError) as exc:
        print(f"\nCould not reach the server on {args.host}:{args.port}: {exc}", flush=True)
        return 2


if __name__ == "__main__":
    sys.exit(main())
