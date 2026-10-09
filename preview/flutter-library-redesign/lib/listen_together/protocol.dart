/// Listen Together wire format.
///
/// The client and the room server exchange protobuf messages inside an
/// `Envelope` over a single WebSocket:
///
/// ```
/// Envelope { string type = 1; bytes payload = 2; bool compressed = 3; }
/// ```
///
/// The field numbers here mirror `metroproto/listentogether.proto` and the
/// encoders in `metroserver-worker/src/index.js`, so this file is the only
/// place that knows the byte layout.
///
/// Payloads may arrive gzipped with `Envelope.compressed = true` — the deployed
/// Worker does this to anything above its size threshold, regardless of the
/// capability the client advertised (its `join_approved` carries the whole room
/// state, so it is always over). Ignoring that flag is why a guest used to land
/// in a room with no code, no host and no members: every field read as missing.
/// Inflating is therefore part of decoding, not an optimisation.
///
/// Dart on the web has no `int64` bitwise arithmetic (int is a double, and
/// `<<` truncates to 32 bits), so both the writer and the reader do their
/// varint arithmetic with `~/` and `*` rather than shifts.
library;

import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';

/// Message types, matching `MessageTypes` in the app's `Protocol.kt`.
abstract final class Lt {
  // Client -> server.
  static const String ping = 'ping';
  static const String clientCapabilities = 'client_capabilities';
  static const String createRoom = 'create_room';
  static const String joinRoom = 'join_room';
  static const String approveJoin = 'approve_join';
  static const String rejectJoin = 'reject_join';
  static const String leaveRoom = 'leave_room';
  static const String playbackAction = 'playback_action';
  static const String requestSync = 'request_sync';
  static const String reconnect = 'reconnect';
  static const String transferHost = 'transfer_host';
  static const String kickUser = 'kick_user';

  // Server -> client.
  static const String pong = 'pong';
  static const String serverCapabilities = 'server_capabilities';
  static const String roomCreated = 'room_created';
  static const String joinRequest = 'join_request';
  static const String joinApproved = 'join_approved';
  static const String joinRejected = 'join_rejected';
  static const String userJoined = 'user_joined';
  static const String userLeft = 'user_left';
  static const String userDisconnected = 'user_disconnected';
  static const String userReconnected = 'user_reconnected';
  static const String syncPlayback = 'sync_playback';
  static const String syncState = 'sync_state';
  static const String hostChanged = 'host_changed';
  static const String kicked = 'kicked';
  static const String error = 'error';
}

/// Playback actions, matching `PlaybackActions` in `Protocol.kt`.
abstract final class LtAction {
  static const String play = 'play';
  static const String pause = 'pause';
  static const String seek = 'seek';
  static const String skipNext = 'skip_next';
  static const String skipPrev = 'skip_prev';
}

/// Error codes the server sends in an `ErrorPayload.code`, as lowercase
/// snake_case (`metroserver/metro_server.py` defines them as `E_ROOM_NOT_FOUND`
/// and the wire value is `room_not_found`; the bundled smoke test asserts
/// `not_host` verbatim).
abstract final class LtError {
  static const String invalidMessage = 'invalid_message';
  static const String roomNotFound = 'room_not_found';
  static const String roomFull = 'room_full';
  static const String roomLimit = 'room_limit';
  static const String notHost = 'not_host';
  static const String notInRoom = 'not_in_room';
  static const String unknownUser = 'unknown_user';
  static const String sessionNotFound = 'session_not_found';
  /// Client-side: the socket never answered, or the address was unreachable.
  static const String noAnswer = 'no_answer';
  static const String unreachable = 'unreachable';
}

// ---------------------------------------------------------------------------
// Writing
// ---------------------------------------------------------------------------

/// Builds one protobuf message. Proto3 has no field presence, so a zero value
/// is simply not written — which is exactly what the Kotlin encoder and the
/// Durable Object's encoder do.
class PbWriter {
  final BytesBuilder _out = BytesBuilder(copy: false);

  void _varint(int value) {
    var v = value;
    while (v >= 128) {
      _out.addByte((v % 128) + 128);
      v = v ~/ 128;
    }
    _out.addByte(v);
  }

  void _tag(int field, int wire) => _varint(field * 8 + wire);

  /// Length-delimited string field. Empty strings are omitted.
  void string(int field, String? value) {
    if (value == null || value.isEmpty) return;
    final List<int> bytes = utf8.encode(value);
    _tag(field, 2);
    _varint(bytes.length);
    _out.add(bytes);
  }

  /// Length-delimited bytes field \u2014 also how a nested message is written.
  void message(int field, List<int> bytes) {
    if (bytes.isEmpty) return;
    _tag(field, 2);
    _varint(bytes.length);
    _out.add(bytes);
  }

  void uint(int field, int value) {
    if (value == 0) return;
    _tag(field, 0);
    _varint(value);
  }

  void boolean(int field, bool value) {
    if (!value) return;
    _tag(field, 0);
    _out.addByte(1);
  }

  Uint8List toBytes() => _out.toBytes();
}

/// Wraps [payload] in the `Envelope` the server expects.
Uint8List envelope(String type, List<int> payload) {
  final PbWriter writer = PbWriter()
    ..string(1, type)
    ..message(2, payload is Uint8List ? payload : Uint8List.fromList(payload));
  return writer.toBytes();
}

// ---------------------------------------------------------------------------
// Reading
// ---------------------------------------------------------------------------

class PbField {
  const PbField(this.number, this.wireType, this.value);

  final int number;
  final int wireType;
  final Object? value;
}

class PbReader {
  PbReader(this._data) : _end = _data.length;

  final Uint8List _data;
  int _pos = 0;
  final int _end;

  bool get isDone => _pos >= _end;

  int _varint() {
    var value = 0;
    var multiplier = 1;
    while (true) {
      if (_pos >= _end) throw const FormatException('truncated varint');
      final int byte = _data[_pos++];
      value += (byte & 0x7f) * multiplier;
      if (byte < 0x80) return value;
      multiplier *= 128;
    }
  }

  PbField next() {
    final int tag = _varint();
    final int number = tag ~/ 8;
    final int wireType = tag % 8;
    switch (wireType) {
      case 0:
        return PbField(number, wireType, _varint());
      case 1:
        final int start = _pos;
        _pos += 8;
        return PbField(number, wireType, _slice(start, 8));
      case 2:
        final int length = _varint();
        final int start = _pos;
        _pos += length;
        return PbField(number, wireType, _slice(start, length));
      case 5:
        final int start = _pos;
        _pos += 4;
        final Uint8List fixed = _slice(start, 4);
        return PbField(
          number,
          wireType,
          fixed.buffer
              .asByteData(fixed.offsetInBytes)
              .getFloat32(0, Endian.little),
        );
      default:
        throw FormatException('unsupported wire type $wireType');
    }
  }

  Uint8List _slice(int start, int length) => Uint8List.view(
        _data.buffer,
        _data.offsetInBytes + start,
        length,
      );

  List<PbField> readAll() {
    final List<PbField> fields = <PbField>[];
    while (!isDone) {
      fields.add(next());
    }
    return fields;
  }
}

/// A decoded protobuf message: every field, aggregated by number so repeated
/// fields (users, queue) come back as a list.
class PbMessage {
  PbMessage();

  factory PbMessage.parse(List<int> bytes) {
    final PbMessage message = PbMessage();
    final Uint8List data =
        bytes is Uint8List ? bytes : Uint8List.fromList(bytes);
    for (final PbField field in PbReader(data).readAll()) {
      (message._fields[field.number] ??= <Object?>[]).add(field.value);
    }
    return message;
  }

  /// Decodes `Envelope` framing and answers the message type plus payload,
  /// inflating the payload when the envelope says it was gzipped.
  ///
  /// If a gzipped payload cannot be inflated the raw bytes are used rather than
  /// throwing, so one malformed frame cannot take the socket handler down.
  static (String, PbMessage) decodeEnvelope(List<int> bytes) {
    final PbMessage outer = PbMessage.parse(bytes);
    final String type = outer.string(1);
    final Uint8List? raw = outer.bytes(2);
    if (raw == null) return (type, PbMessage());
    final Uint8List payload =
        outer.boolean(3) ? (gunzip(raw) ?? raw) : raw;
    return (type, PbMessage.parse(payload));
  }

  /// Gzip, with `null` when the bytes are not a valid member.
  static Uint8List? gunzip(List<int> bytes) {
    try {
      return Uint8List.fromList(GZipDecoder().decodeBytes(bytes));
    } on Exception {
      return null;
    }
  }

  final Map<int, List<Object?>> _fields = <int, List<Object?>>{};

  bool has(int field) => _fields.containsKey(field);

  Object? _last(int field) {
    final List<Object?>? values = _fields[field];
    return (values == null || values.isEmpty) ? null : values.last;
  }

  String string(int field, [String fallback = '']) {
    final Object? value = _last(field);
    if (value is Uint8List) return utf8.decode(value, allowMalformed: true);
    return fallback;
  }

  int integer(int field, [int fallback = 0]) {
    final Object? value = _last(field);
    return value is int ? value : fallback;
  }

  bool boolean(int field, [bool fallback = false]) {
    final Object? value = _last(field);
    return value is int ? value != 0 : fallback;
  }

  double decimal(int field, [double fallback = 0]) {
    final Object? value = _last(field);
    if (value is double) return value;
    if (value is int) return value.toDouble();
    return fallback;
  }

  Uint8List? bytes(int field) {
    final Object? value = _last(field);
    return value is Uint8List ? value : null;
  }

  PbMessage? message(int field) {
    final Uint8List? value = bytes(field);
    return value == null ? null : PbMessage.parse(value);
  }

  List<String> strings(int field) => <String>[
        for (final Object? value in _fields[field] ?? const <Object?>[])
          if (value is Uint8List) utf8.decode(value, allowMalformed: true),
      ];

  List<PbMessage> messages(int field) => <PbMessage>[
        for (final Object? value in _fields[field] ?? const <Object?>[])
          if (value is Uint8List) PbMessage.parse(value),
      ];
}

// ---------------------------------------------------------------------------
// Payload builders
// ---------------------------------------------------------------------------

Uint8List createRoomPayload(String username) =>
    (PbWriter()..string(1, username)).toBytes();

Uint8List joinRoomPayload(String roomCode, String username) =>
    (PbWriter()
          ..string(1, roomCode)
          ..string(2, username))
        .toBytes();

Uint8List approveJoinPayload(String userId) =>
    (PbWriter()..string(1, userId)).toBytes();

Uint8List rejectJoinPayload(String userId, [String reason = 'Host declined']) =>
    (PbWriter()
          ..string(1, userId)
          ..string(2, reason))
        .toBytes();

Uint8List transferHostPayload(String userId) =>
    (PbWriter()..string(1, userId)).toBytes();

Uint8List kickUserPayload(String userId, [String reason = 'Removed by host']) =>
    (PbWriter()
          ..string(1, userId)
          ..string(2, reason))
        .toBytes();

Uint8List pingPayload(int clientTimeMs, int sequence) =>
    (PbWriter()
          ..uint(1, clientTimeMs)
          ..uint(2, sequence))
        .toBytes();

/// The capability handshake: answers with `server_capabilities`, and is how the
/// settings screen tests an address without needing CORS on the health route.
Uint8List clientCapabilitiesPayload({required String clientVersion}) =>
    (PbWriter()
          ..boolean(1, true)
          ..boolean(2, false)
          ..string(3, clientVersion))
        .toBytes();

Uint8List playbackPayload(String action, {String? trackId, int? position}) =>
    (PbWriter()
          ..string(1, action)
          ..string(2, trackId)
          ..uint(3, position ?? 0))
        .toBytes();

/// Nested `TrackInfo`, as used by `sync_state`.
Uint8List trackPayload({
  required String id,
  required String title,
  required String artist,
  String? album,
  int? duration,
  String? thumbnail,
}) =>
    (PbWriter()
          ..string(1, id)
          ..string(2, title)
          ..string(3, artist)
          ..string(4, album)
          ..uint(5, duration ?? 0)
          ..string(6, thumbnail))
        .toBytes();
