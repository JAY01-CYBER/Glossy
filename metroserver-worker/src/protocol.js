/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Listen Together wire protocol: the protobuf subset, the gzip envelope and the
 * room models.
 *
 * Ported field for field from `metroserver/metro_server.py`, which is the
 * reference implementation both the Android client and its smoke test agree
 * with. Nothing here touches Worker APIs, so it can be exercised in plain Node.
 */

// ---------------------------------------------------------------------------
// Message types
// ---------------------------------------------------------------------------

// Client -> server
export const M_CREATE_ROOM = 'create_room';
export const M_JOIN_ROOM = 'join_room';
export const M_LEAVE_ROOM = 'leave_room';
export const M_APPROVE_JOIN = 'approve_join';
export const M_REJECT_JOIN = 'reject_join';
export const M_PLAYBACK_ACTION = 'playback_action';
export const M_BUFFER_READY = 'buffer_ready';
export const M_KICK_USER = 'kick_user';
export const M_TRANSFER_HOST = 'transfer_host';
export const M_PING = 'ping';
export const M_CHAT = 'chat';
export const M_REQUEST_SYNC = 'request_sync';
export const M_RECONNECT = 'reconnect';
export const M_SUGGEST_TRACK = 'suggest_track';
export const M_APPROVE_SUGGESTION = 'approve_suggestion';
export const M_REJECT_SUGGESTION = 'reject_suggestion';
// The client never sends this one, but a future client may.
export const M_CLIENT_CAPABILITIES = 'client_capabilities';

// Server -> client
export const S_ROOM_CREATED = 'room_created';
export const S_JOIN_REQUEST = 'join_request';
export const S_JOIN_APPROVED = 'join_approved';
export const S_JOIN_REJECTED = 'join_rejected';
export const S_USER_JOINED = 'user_joined';
export const S_USER_LEFT = 'user_left';
export const S_SYNC_PLAYBACK = 'sync_playback';
export const S_BUFFER_WAIT = 'buffer_wait';
export const S_BUFFER_COMPLETE = 'buffer_complete';
export const S_ERROR = 'error';
export const S_PONG = 'pong';
export const S_HOST_CHANGED = 'host_changed';
export const S_KICKED = 'kicked';
export const S_SYNC_STATE = 'sync_state';
export const S_RECONNECTED = 'reconnected';
export const S_USER_RECONNECTED = 'user_reconnected';
export const S_USER_DISCONNECTED = 'user_disconnected';
export const S_SUGGESTION_RECEIVED = 'suggestion_received';
export const S_SUGGESTION_APPROVED = 'suggestion_approved';
export const S_SUGGESTION_REJECTED = 'suggestion_rejected';
export const S_SERVER_CAPABILITIES = 'server_capabilities';

// Playback actions
export const A_PLAY = 'play';
export const A_PAUSE = 'pause';
export const A_SEEK = 'seek';
export const A_SKIP_NEXT = 'skip_next';
export const A_SKIP_PREV = 'skip_prev';
export const A_CHANGE_TRACK = 'change_track';
export const A_QUEUE_ADD = 'queue_add';
export const A_QUEUE_REMOVE = 'queue_remove';
export const A_QUEUE_CLEAR = 'queue_clear';
export const A_SYNC_QUEUE = 'sync_queue';
export const A_SET_VOLUME = 'set_volume';

export const PLAYBACK_ACTIONS = new Set([
  A_PLAY, A_PAUSE, A_SEEK, A_SKIP_NEXT, A_SKIP_PREV, A_CHANGE_TRACK,
  A_QUEUE_ADD, A_QUEUE_REMOVE, A_QUEUE_CLEAR, A_SYNC_QUEUE, A_SET_VOLUME,
]);

// Errors
export const E_INVALID_MESSAGE = 'invalid_message';
export const E_ROOM_NOT_FOUND = 'room_not_found';
export const E_ROOM_FULL = 'room_full';
export const E_ROOM_LIMIT = 'room_limit';
export const E_NOT_HOST = 'not_host';
export const E_NOT_IN_ROOM = 'not_in_room';
export const E_SESSION_NOT_FOUND = 'session_not_found';
export const E_UNKNOWN_USER = 'unknown_user';

// Limits, mirroring the reference server.
export const COMPRESSION_THRESHOLD = 100;
export const MAX_USERS_PER_ROOM = 16;
export const MAX_ROOMS = 5000;
export const SESSION_TTL_MS = 120_000;
export const EMPTY_ROOM_TTL_MS = 600_000;
export const ROOM_CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
export const ROOM_CODE_LENGTH = 6;

export const nowMs = () => Date.now();

// ---------------------------------------------------------------------------
// Minimal protobuf writer / reader (only the subset the protocol uses)
// ---------------------------------------------------------------------------

export function concat(...chunks) {
  let total = 0;
  for (const chunk of chunks) total += chunk.length;
  const out = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

export function varint(value) {
  if (value < 0) throw new RangeError('protobuf varints here are unsigned');
  const out = [];
  let remaining = value;
  for (;;) {
    const byte = remaining % 128;
    remaining = Math.floor(remaining / 128);
    out.push(remaining ? byte | 0x80 : byte);
    if (!remaining) break;
  }
  return new Uint8Array(out);
}

function tag(field, wire) {
  return varint((field << 3) | wire);
}

/** proto3 default (zero) is omitted, exactly like the reference server. */
export function pbVarint(field, value) {
  return value ? concat(tag(field, 0), varint(value)) : new Uint8Array(0);
}

export function pbBool(field, value) {
  return value ? concat(tag(field, 0), new Uint8Array([1])) : new Uint8Array(0);
}

export function pbString(field, value) {
  if (!value) return new Uint8Array(0);
  const data = new TextEncoder().encode(value);
  return concat(tag(field, 2), varint(data.length), data);
}

export function pbBytes(field, value) {
  if (!value || !value.length) return new Uint8Array(0);
  return concat(tag(field, 2), varint(value.length), value);
}

export function pbMessage(field, value) {
  if (!value || !value.length) return new Uint8Array(0);
  return concat(tag(field, 2), varint(value.length), value);
}

/** Always written, even at zero: RoomState.volume is non-nullable. */
export function pbFloat(field, value) {
  const body = new Uint8Array(4);
  new DataView(body.buffer).setFloat32(0, Number(value) || 0, true);
  return concat(tag(field, 5), body);
}

export function pbFloatOpt(field, value) {
  if (!value) return new Uint8Array(0);
  return pbFloat(field, value);
}

function readVarint(data, index) {
  let result = 0;
  let shift = 0;
  let cursor = index;
  for (;;) {
    if (cursor >= data.length) throw new Error('truncated varint');
    const byte = data[cursor++];
    result += (byte & 0x7f) * 2 ** shift;
    if (!(byte & 0x80)) return [result, cursor];
    shift += 7;
    if (shift > 63) throw new Error('varint too long');
  }
}

/** Parsed protobuf fields: (field, wire, value) with value a number or bytes. */
export class Fields {
  constructor(data) {
    this.items = [];
    let index = 0;
    while (index < data.length) {
      let key;
      [key, index] = readVarint(data, index);
      const field = key >> 3;
      const wire = key & 7;
      let value;
      if (wire === 0) {
        [value, index] = readVarint(data, index);
      } else if (wire === 1) {
        value = data.subarray(index, index + 8);
        index += 8;
      } else if (wire === 2) {
        let length;
        [length, index] = readVarint(data, index);
        value = data.subarray(index, index + length);
        index += length;
      } else if (wire === 5) {
        value = data.subarray(index, index + 4);
        index += 4;
      } else {
        throw new Error(`unsupported wire type ${wire}`);
      }
      this.items.push([field, wire, value]);
    }
  }

  string(field, fallback = '') {
    for (const [f, w, v] of this.items) if (f === field && w === 2) return decodeText(v);
    return fallback;
  }

  raw(field) {
    for (const [f, w, v] of this.items) if (f === field && w === 2) return v;
    return null;
  }

  number(field, fallback = 0) {
    for (const [f, w, v] of this.items) if (f === field && w === 0) return v;
    return fallback;
  }

  boolean(field) {
    return Boolean(this.number(field));
  }

  float(field, fallback = 0) {
    for (const [f, w, v] of this.items) {
      if (f === field && w === 5) return new DataView(v.buffer, v.byteOffset, v.byteLength).getFloat32(0, true);
    }
    return fallback;
  }

  /** All occurrences of a length-delimited field, in order. */
  repeated(field) {
    const out = [];
    for (const [f, w, v] of this.items) if (f === field && w === 2) out.push(v);
    return out;
  }
}

const textDecoder = new TextDecoder('utf-8');
function decodeText(bytes) {
  return textDecoder.decode(bytes);
}

// ---------------------------------------------------------------------------
// Envelope
// ---------------------------------------------------------------------------

async function gzip(bytes) {
  const stream = new Blob([bytes]).stream().pipeThrough(new CompressionStream('gzip'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

async function gunzip(bytes) {
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

/**
 * Wrap a payload the way the client's MessageCodec does: gzip only above the
 * 100-byte threshold and only when it actually shrinks the payload.
 */
export async function encodeEnvelope(msgType, payload = new Uint8Array(0)) {
  if (payload.length > COMPRESSION_THRESHOLD) {
    const packed = await gzip(payload);
    if (packed.length < payload.length) {
      return concat(pbString(1, msgType), pbBytes(2, packed), pbBool(3, true));
    }
  }
  return concat(pbString(1, msgType), pbBytes(2, payload));
}

export async function decodeEnvelope(data) {
  const fields = new Fields(data);
  const msgType = fields.string(1);
  let payload = fields.raw(2) || new Uint8Array(0);
  if (fields.boolean(3)) payload = await gunzip(payload);
  return [msgType, payload];
}

// ---------------------------------------------------------------------------
// Models
// ---------------------------------------------------------------------------

export class Track {
  constructor(init = {}) {
    this.id = init.id || '';
    this.title = init.title || '';
    this.artist = init.artist || '';
    this.album = init.album || '';
    this.duration = init.duration || 0;
    this.thumbnail = init.thumbnail || '';
    this.suggestedBy = init.suggestedBy || '';
  }

  static parse(data) {
    const fields = new Fields(data);
    return new Track({
      id: fields.string(1),
      title: fields.string(2),
      artist: fields.string(3),
      album: fields.string(4),
      duration: fields.number(5),
      thumbnail: fields.string(6),
      suggestedBy: fields.string(7),
    });
  }

  encode() {
    return concat(
      pbString(1, this.id),
      pbString(2, this.title),
      pbString(3, this.artist),
      pbString(4, this.album),
      pbVarint(5, this.duration),
      pbString(6, this.thumbnail),
      pbString(7, this.suggestedBy),
    );
  }

  toJSON() {
    return {
      id: this.id, title: this.title, artist: this.artist,
      album: this.album, duration: this.duration, thumbnail: this.thumbnail,
    };
  }
}

export class Member {
  constructor({ userId, username, token, isHost = false, roomCode = '' }) {
    this.userId = userId;
    this.username = username;
    this.token = token;
    this.isHost = isHost;
    this.connected = true;
    this.roomCode = roomCode;
    this.ws = null;
    this.disconnectedAt = null;
  }

  encode() {
    return concat(
      pbString(1, this.userId),
      pbString(2, this.username),
      pbBool(3, this.isHost),
      pbBool(4, this.connected),
    );
  }
}

export class Room {
  constructor(code, hostId) {
    this.code = code;
    this.hostId = hostId;
    this.members = new Map();
    this.currentTrack = null;
    this.isPlaying = false;
    this.position = 0;
    this.lastUpdate = nowMs();
    this.volume = 1.0;
    this.queue = [];
    this.revision = 1;
    this.waitingFor = new Set();
    this.pendingJoiners = new Map();
    this.suggestions = new Map();
    this.emptySince = null;
  }

  stateMessage() {
    const parts = [pbString(1, this.code), pbString(2, this.hostId)];
    for (const member of this.members.values()) parts.push(pbMessage(3, member.encode()));
    if (this.currentTrack) parts.push(pbMessage(4, this.currentTrack.encode()));
    parts.push(pbBool(5, this.isPlaying));
    parts.push(pbVarint(6, Math.max(0, Math.round(this.effectivePosition()))));
    parts.push(pbVarint(7, this.lastUpdate));
    parts.push(pbFloat(8, this.volume));
    for (const queued of this.queue) parts.push(pbMessage(9, queued.encode()));
    parts.push(pbVarint(10, this.revision));
    return concat(...parts);
  }

  syncStateMessage() {
    const parts = [];
    if (this.currentTrack) parts.push(pbMessage(1, this.currentTrack.encode()));
    parts.push(pbBool(2, this.isPlaying));
    parts.push(pbVarint(3, Math.max(0, Math.round(this.effectivePosition()))));
    parts.push(pbVarint(4, this.lastUpdate));
    for (const queued of this.queue) parts.push(pbMessage(5, queued.encode()));
    parts.push(pbFloat(6, this.volume));
    parts.push(pbVarint(7, this.revision));
    return concat(...parts);
  }

  /** Playback position now, accounting for the time since the last update. */
  effectivePosition() {
    if (!this.isPlaying) return this.position;
    return this.position + Math.max(0, nowMs() - this.lastUpdate);
  }

  toJSON() {
    return {
      code: this.code,
      host: this.hostId,
      users: this.members.size,
      playing: this.isPlaying,
      revision: this.revision,
      track: this.currentTrack ? this.currentTrack.title : null,
    };
  }
}

/** Applies a host's playback action to the room, mirroring the reference. */
export function applyAction(room, action, { trackId, position, trackInfo, insertNext, queue, volume }) {
  switch (action) {
    case A_PLAY:
      room.isPlaying = true;
      if (position) room.position = position;
      break;
    case A_PAUSE:
      room.isPlaying = false;
      room.position = position || Math.round(room.effectivePosition());
      break;
    case A_SEEK:
      room.position = position;
      break;
    case A_SKIP_NEXT:
    case A_SKIP_PREV:
      if (room.queue.length) {
        room.currentTrack = action === A_SKIP_NEXT ? room.queue.shift() : room.queue.pop();
      }
      room.position = 0;
      room.isPlaying = true;
      break;
    case A_CHANGE_TRACK:
      if (trackInfo) room.currentTrack = Track.parse(trackInfo);
      room.position = 0;
      room.isPlaying = false;
      if (queue.length) room.queue = queue;
      break;
    case A_QUEUE_ADD:
      if (trackInfo) {
        const added = Track.parse(trackInfo);
        if (insertNext) room.queue.unshift(added);
        else room.queue.push(added);
      }
      break;
    case A_QUEUE_REMOVE:
      room.queue = room.queue.filter((track) => track.id !== trackId);
      break;
    case A_QUEUE_CLEAR:
      room.queue = [];
      break;
    case A_SYNC_QUEUE:
      room.queue = queue;
      break;
    case A_SET_VOLUME:
      room.volume = Math.max(0, Math.min(1, volume));
      break;
    default:
      break;
  }
}

/** Six characters from an alphabet without look-alikes (I, O, 0, 1). */
export function newRoomCode(exists) {
  for (;;) {
    let code = '';
    const random = crypto.getRandomValues(new Uint8Array(ROOM_CODE_LENGTH));
    for (let i = 0; i < ROOM_CODE_LENGTH; i++) {
      code += ROOM_CODE_ALPHABET[random[i] % ROOM_CODE_ALPHABET.length];
    }
    if (!exists(code)) return code;
  }
}

export function randomUserId() {
  return crypto.randomUUID().replace(/-/g, '').slice(0, 12);
}

export function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(18));
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
