/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Listen Together room server on Cloudflare Workers.
 *
 * The Android client speaks binary protobuf over a single WebSocket, and every
 * socket in a room has to see the same room state. A plain Worker cannot hold
 * that state or relay between two sockets, so all connections go to one
 * Durable Object ("hub") that owns every room — the same shape as the
 * single-process reference server in `metroserver/metro_server.py`.
 *
 * The DO uses the WebSocket Hibernation API: idle sockets cost nothing while
 * no messages flow, and each socket carries its binding (user, room, token) as
 * a serialized attachment so it survives an eviction. Room state is written
 * through to DO storage after every mutation and reloaded on wake.
 *
 *   GET /            health JSON
 *   GET /ws (+Upgrade: websocket)   the room protocol
 */

import * as P from './protocol.js';

const HUB_NAME = 'hub';
const SNAPSHOT_KEY = 'state';

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const upgrade = (request.headers.get('Upgrade') || '').toLowerCase();

    if (upgrade !== 'websocket' || !url.pathname.startsWith('/ws')) {
      return health(request, env);
    }

    const id = env.ROOM_HUB.idFromName(HUB_NAME);
    return env.ROOM_HUB.get(id).fetch(request);
  },
};

/** Anything that is not a websocket upgrade answers with the server's numbers. */
async function health(request, env) {
  let stats = { rooms: 0, clients: 0 };
  try {
    const id = env.ROOM_HUB.idFromName(HUB_NAME);
    const response = await env.ROOM_HUB.get(id).fetch('https://hub.internal/stats');
    stats = await response.json();
  } catch (error) {
    stats = { rooms: 0, clients: 0, error: String(error && error.message ? error.message : error) };
  }
  return Response.json({
    status: 'ok',
    server: 'glossy-listen-together-worker',
    rooms: stats.rooms,
    clients: stats.clients,
    roomList: stats.roomList || [],
  });
}

export class RoomHub {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.rooms = new Map();
    this.sessions = new Map();
    this.startedAt = P.nowMs();
    this.ready = this.#load();
  }

  // -- lifecycle ----------------------------------------------------------

  async #load() {
    const snapshot = await this.ctx.storage.get(SNAPSHOT_KEY);
    if (!snapshot) return;
    this.startedAt = snapshot.startedAt || this.startedAt;
    this.sessions = new Map((snapshot.sessions || []).map(([token, value]) => [token, value]));

    for (const raw of snapshot.rooms || []) {
      const room = new P.Room(raw.code, raw.hostId);
      room.isPlaying = Boolean(raw.isPlaying);
      room.position = raw.position || 0;
      room.lastUpdate = raw.lastUpdate || P.nowMs();
      room.volume = typeof raw.volume === 'number' ? raw.volume : 1;
      room.revision = raw.revision || 1;
      room.currentTrack = raw.currentTrack ? new P.Track(raw.currentTrack) : null;
      room.queue = (raw.queue || []).map((track) => new P.Track(track));
      room.suggestions = new Map(raw.suggestions || []);
      room.emptySince = raw.emptySince ?? null;
      for (const member of raw.members || []) {
        const rebuilt = new P.Member(member);
        rebuilt.connected = Boolean(member.connected);
        rebuilt.disconnectedAt = member.disconnectedAt ?? null;
        room.members.set(rebuilt.userId, rebuilt);
      }
      this.rooms.set(room.code, room);
    }

    // Sockets outlive an eviction; their attachments are the record of who they
    // are, so a live socket is re-linked to the member it belongs to.
    for (const ws of this.ctx.getWebSockets()) {
      const attachment = ws.deserializeAttachment();
      if (!attachment || !attachment.roomCode) continue;
      const room = this.rooms.get(attachment.roomCode);
      if (!room) continue;
      if (attachment.pending) {
        room.pendingJoiners.set(attachment.userId, {
          userId: attachment.userId,
          username: attachment.username || 'Guest',
          ws,
        });
        continue;
      }
      const member = room.members.get(attachment.userId);
      if (member) {
        member.ws = ws;
        member.connected = true;
        member.disconnectedAt = null;
      }
    }
  }

  async #persist() {
    await this.ctx.storage.put(SNAPSHOT_KEY, {
      startedAt: this.startedAt,
      sessions: [...this.sessions.entries()],
      rooms: [...this.rooms.values()].map((room) => ({
        code: room.code,
        hostId: room.hostId,
        isPlaying: room.isPlaying,
        position: room.position,
        lastUpdate: room.lastUpdate,
        volume: room.volume,
        revision: room.revision,
        emptySince: room.emptySince,
        currentTrack: room.currentTrack ? { ...room.currentTrack } : null,
        queue: room.queue.map((track) => ({ ...track })),
        suggestions: [...room.suggestions.entries()],
        members: [...room.members.values()].map((member) => ({
          userId: member.userId,
          username: member.username,
          token: member.token,
          isHost: member.isHost,
          connected: member.connected,
          disconnectedAt: member.disconnectedAt,
          roomCode: member.roomCode,
        })),
      })),
    });
  }

  async fetch(request) {
    await this.ready;
    const url = new URL(request.url);

    if (url.pathname === '/stats') {
      return Response.json({
        rooms: this.rooms.size,
        clients: this.rooms.size ? [...this.rooms.values()].reduce((sum, room) => sum + room.members.size, 0) : 0,
        sessions: this.sessions.size,
        uptimeSeconds: Math.floor((P.nowMs() - this.startedAt) / 1000),
        roomList: [...this.rooms.values()].slice(0, 50).map((room) => room.toJSON()),
      });
    }

    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    // `acceptWebSocket` opts this socket into hibernation: while the room is
    // quiet the DO can be evicted, and the attachment below brings the socket
    // back to its member.
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }

  // -- socket plumbing ----------------------------------------------------

  async webSocketMessage(ws, message) {
    await this.ready;
    let bytes;
    if (message instanceof ArrayBuffer) {
      bytes = new Uint8Array(message);
    } else if (ArrayBuffer.isView(message)) {
      bytes = new Uint8Array(message.buffer, message.byteOffset, message.byteLength);
    } else {
      // Text frames are not part of the protocol.
      return;
    }

    let type;
    let payload;
    try {
      [type, payload] = await P.decodeEnvelope(bytes);
    } catch (error) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, `bad envelope: ${error.message}`);
      return;
    }

    try {
      await this.#dispatch(ws, type, payload);
    } catch (error) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, String((error && error.message) || error));
    }
  }

  async webSocketClose(ws) {
    await this.ready;
    await this.#onDisconnect(ws);
  }

  async webSocketError(ws) {
    await this.ready;
    await this.#onDisconnect(ws);
  }

  async alarm() {
    await this.ready;
    await this.#sweep();
  }

  async #send(ws, type, payload = new Uint8Array(0)) {
    try {
      ws.send(await P.encodeEnvelope(type, payload));
    } catch {
      // A socket that has gone away needs no further messages.
    }
  }

  async #sendError(ws, code, message) {
    await this.#send(ws, P.S_ERROR, P.concat(P.pbString(1, code), P.pbString(2, message)));
  }

  async #broadcast(room, type, payload, exclude = null) {
    for (const member of room.members.values()) {
      if (!member.ws || member.ws === exclude) continue;
      await this.#send(member.ws, type, payload);
    }
  }

  #attachment(ws) {
    return ws.deserializeAttachment() || null;
  }

  #room(ws) {
    const attachment = this.#attachment(ws);
    if (!attachment || !attachment.roomCode || attachment.pending) return null;
    return this.rooms.get(attachment.roomCode) || null;
  }

  #member(ws) {
    const room = this.#room(ws);
    if (!room) return null;
    const attachment = this.#attachment(ws);
    return room.members.get(attachment.userId) || null;
  }

  #bind(ws, member) {
    ws.serializeAttachment({
      userId: member.userId,
      username: member.username,
      roomCode: member.roomCode,
      token: member.token,
      isHost: member.isHost,
    });
  }

  async #onDisconnect(ws) {
    const attachment = this.#attachment(ws);
    if (!attachment) return;

    if (attachment.pending) {
      const room = this.rooms.get(attachment.roomCode);
      if (room) room.pendingJoiners.delete(attachment.userId);
      return;
    }

    const room = this.rooms.get(attachment.roomCode);
    const member = room && room.members.get(attachment.userId);
    if (!member) return;

    member.connected = false;
    member.disconnectedAt = P.nowMs();
    member.ws = null;
    await this.#broadcast(room, P.S_USER_DISCONNECTED, P.concat(
      P.pbString(1, member.userId),
      P.pbString(2, member.username),
    ));
    await this.#persist();
    await this.#scheduleSweep();
  }

  /** Expiry is alarm-driven: a dropped guest keeps their seat for a while. */
  async #scheduleSweep() {
    const deadlines = [];
    for (const room of this.rooms.values()) {
      for (const member of room.members.values()) {
        if (!member.connected && member.disconnectedAt) {
          deadlines.push(member.disconnectedAt + P.SESSION_TTL_MS);
        }
      }
      if (room.members.size === 0 && room.emptySince) {
        deadlines.push(room.emptySince + P.EMPTY_ROOM_TTL_MS);
      }
    }
    if (deadlines.length) await this.ctx.storage.setAlarm(Math.min(...deadlines));
  }

  async #sweep() {
    const now = P.nowMs();
    let changed = false;

    for (const room of [...this.rooms.values()]) {
      for (const member of [...room.members.values()]) {
        if (member.connected || !member.disconnectedAt) continue;
        if (now - member.disconnectedAt < P.SESSION_TTL_MS) continue;
        room.members.delete(member.userId);
        this.sessions.delete(member.token);
        changed = true;
        await this.#broadcast(room, P.S_USER_LEFT, P.concat(
          P.pbString(1, member.userId),
          P.pbString(2, member.username),
        ));
        await this.#afterMemberRemoved(room, member);
      }

      if (room.members.size === 0) {
        if (room.emptySince == null) room.emptySince = now;
        if (now - room.emptySince >= P.EMPTY_ROOM_TTL_MS) {
          this.rooms.delete(room.code);
          changed = true;
        }
      }
    }

    if (changed) await this.#persist();
    await this.#scheduleSweep();
  }

  // -- dispatch -----------------------------------------------------------

  async #dispatch(ws, type, payload) {
    switch (type) {
      case P.M_PING: return this.#onPing(ws, payload);
      case P.M_CREATE_ROOM: return this.#onCreateRoom(ws, payload);
      case P.M_JOIN_ROOM: return this.#onJoinRoom(ws, payload);
      case P.M_APPROVE_JOIN: return this.#onApproveJoin(ws, payload);
      case P.M_REJECT_JOIN: return this.#onRejectJoin(ws, payload);
      case P.M_LEAVE_ROOM: return this.#onLeaveRoom(ws);
      case P.M_PLAYBACK_ACTION: return this.#onPlaybackAction(ws, payload);
      case P.M_BUFFER_READY: return this.#onBufferReady(ws, payload);
      case P.M_KICK_USER: return this.#onKickUser(ws, payload);
      case P.M_TRANSFER_HOST: return this.#onTransferHost(ws, payload);
      case P.M_REQUEST_SYNC: return this.#onRequestSync(ws);
      case P.M_RECONNECT: return this.#onReconnect(ws, payload);
      case P.M_SUGGEST_TRACK: return this.#onSuggestTrack(ws, payload);
      case P.M_APPROVE_SUGGESTION: return this.#onApproveSuggestion(ws, payload);
      case P.M_REJECT_SUGGESTION: return this.#onRejectSuggestion(ws, payload);
      case P.M_CHAT:
        // The client has no encoder for chat yet; accept and ignore it.
        return undefined;
      case P.M_CLIENT_CAPABILITIES:
        return this.#send(ws, P.S_SERVER_CAPABILITIES, P.concat(
          P.pbBool(1, true),
          P.pbBool(2, true),
          P.pbString(3, 'glossy-room-worker-1.0'),
        ));
      default:
        return this.#sendError(ws, P.E_INVALID_MESSAGE, `unknown type ${type}`);
    }
  }

  // -- handlers -----------------------------------------------------------

  async #onPing(ws, payload) {
    const fields = new P.Fields(payload);
    const clientTime = fields.number(1);
    const sequence = fields.number(2);
    const received = P.nowMs();
    await this.#send(ws, P.S_PONG, P.concat(
      P.pbVarint(1, clientTime),
      P.pbVarint(2, received),
      P.pbVarint(3, P.nowMs()),
      P.pbVarint(4, sequence),
    ));
  }

  async #onCreateRoom(ws, payload) {
    if (this.#room(ws)) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, 'already in a room');
      return;
    }
    if (this.rooms.size >= P.MAX_ROOMS) {
      await this.#sendError(ws, P.E_ROOM_LIMIT, 'server is at capacity');
      return;
    }

    const username = new P.Fields(payload).string(1) || 'Host';
    const userId = P.randomUserId();
    const token = P.randomToken();
    const code = P.newRoomCode((candidate) => this.rooms.has(candidate));

    const room = new P.Room(code, userId);
    const host = new P.Member({ userId, username, token, isHost: true, roomCode: code });
    host.ws = ws;
    room.members.set(userId, host);
    room.emptySince = null;
    this.rooms.set(code, room);
    this.sessions.set(token, { userId, roomCode: code });
    this.#bind(ws, host);

    await this.#send(ws, P.S_ROOM_CREATED, P.concat(
      P.pbString(1, code),
      P.pbString(2, userId),
      P.pbString(3, token),
    ));
    await this.#persist();
  }

  async #onJoinRoom(ws, payload) {
    if (this.#room(ws)) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, 'already in a room');
      return;
    }

    const fields = new P.Fields(payload);
    const code = fields.string(1).trim().toUpperCase();
    const username = fields.string(2) || 'Guest';
    const room = this.rooms.get(code);
    if (!room) {
      await this.#sendError(ws, P.E_ROOM_NOT_FOUND, `no room ${code}`);
      return;
    }
    if (room.members.size >= P.MAX_USERS_PER_ROOM) {
      await this.#sendError(ws, P.E_ROOM_FULL, 'room is full');
      return;
    }

    const userId = P.randomUserId();
    room.pendingJoiners.set(userId, { userId, username, ws });
    ws.serializeAttachment({ userId, username, roomCode: code, pending: true });

    const host = room.members.get(room.hostId);
    if (host && host.ws) {
      await this.#send(host.ws, P.S_JOIN_REQUEST, P.concat(
        P.pbString(1, userId),
        P.pbString(2, username),
      ));
    } else {
      // Nobody to approve the request: let the joiner in directly.
      await this.#admit(room, userId, username, ws);
    }
    await this.#persist();
  }

  async #admit(room, userId, username, ws) {
    const token = P.randomToken();
    const member = new P.Member({ userId, username, token, isHost: false, roomCode: room.code });
    member.ws = ws;
    room.members.set(userId, member);
    room.pendingJoiners.delete(userId);
    room.emptySince = null;
    this.sessions.set(token, { userId, roomCode: room.code });
    this.#bind(ws, member);

    await this.#send(ws, P.S_JOIN_APPROVED, P.concat(
      P.pbString(1, room.code),
      P.pbString(2, userId),
      P.pbString(3, token),
      P.pbMessage(4, room.stateMessage()),
    ));
    await this.#broadcast(room, P.S_USER_JOINED, P.concat(
      P.pbString(1, userId),
      P.pbString(2, username),
    ), ws);
    await this.#persist();
  }

  async #onApproveJoin(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can approve');
      return;
    }
    const userId = new P.Fields(payload).string(1);
    const pending = room.pendingJoiners.get(userId);
    if (!pending) {
      await this.#sendError(ws, P.E_UNKNOWN_USER, 'no pending request');
      return;
    }
    await this.#admit(room, pending.userId, pending.username, pending.ws);
  }

  async #onRejectJoin(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can reject');
      return;
    }
    const fields = new P.Fields(payload);
    const userId = fields.string(1);
    const reason = fields.string(2) || 'Host declined';
    const pending = room.pendingJoiners.get(userId);
    if (!pending) return;
    room.pendingJoiners.delete(userId);
    pending.ws.serializeAttachment(null);
    await this.#send(pending.ws, P.S_JOIN_REJECTED, P.pbString(1, reason));
    await this.#persist();
  }

  async #onLeaveRoom(ws) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member) {
      await this.#sendError(ws, P.E_NOT_IN_ROOM, 'not in a room');
      return;
    }
    room.members.delete(member.userId);
    this.sessions.delete(member.token);
    ws.serializeAttachment(null);
    await this.#broadcast(room, P.S_USER_LEFT, P.concat(
      P.pbString(1, member.userId),
      P.pbString(2, member.username),
    ));
    await this.#afterMemberRemoved(room, member);
    await this.#persist();
  }

  async #afterMemberRemoved(room, member) {
    if (room.members.size === 0) {
      room.emptySince = P.nowMs();
      return;
    }
    if (member.isHost || room.hostId === member.userId) {
      await this.#promoteNewHost(room);
    }
  }

  async #promoteNewHost(room) {
    const members = [...room.members.values()];
    const candidates = members.filter((member) => member.connected);
    const pool = candidates.length ? candidates : members;
    if (!pool.length) return;
    const newHost = pool[0];
    for (const member of room.members.values()) {
      member.isHost = member === newHost;
      if (member.ws) this.#bind(member.ws, member);
    }
    room.hostId = newHost.userId;
    room.revision += 1;
    await this.#broadcast(room, P.S_HOST_CHANGED, P.concat(
      P.pbString(1, newHost.userId),
      P.pbString(2, newHost.username),
    ));
  }

  async #onPlaybackAction(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member) {
      await this.#sendError(ws, P.E_NOT_IN_ROOM, 'not in a room');
      return;
    }
    if (!member.isHost && member.userId !== room.hostId) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host controls playback');
      return;
    }

    const fields = new P.Fields(payload);
    const action = fields.string(1);
    if (!P.PLAYBACK_ACTIONS.has(action)) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, `unknown action ${action}`);
      return;
    }

    const trackId = fields.string(2);
    const position = fields.number(3);
    const trackInfo = fields.raw(4);
    const insertNext = fields.boolean(5);
    const queue = fields.repeated(6).map((raw) => P.Track.parse(raw));
    const queueTitle = fields.string(7);
    const volume = fields.float(8);

    P.applyAction(room, action, { trackId, position, trackInfo, insertNext, queue, volume });

    room.revision += 1;
    const serverTime = P.nowMs();
    room.lastUpdate = serverTime;

    const relayed = action === P.A_SYNC_QUEUE || action === P.A_CHANGE_TRACK ? queue : room.queue;
    const parts = [
      P.pbString(1, action),
      P.pbString(2, trackId),
      P.pbVarint(3, position),
      trackInfo ? P.pbMessage(4, trackInfo) : new Uint8Array(0),
      P.pbBool(5, insertNext),
    ];
    for (const queued of relayed) parts.push(P.pbMessage(6, queued.encode()));
    parts.push(P.pbString(7, queueTitle));
    parts.push(P.pbFloatOpt(8, volume));
    parts.push(P.pbVarint(9, serverTime));
    parts.push(P.pbVarint(10, room.revision));
    parts.push(P.pbVarint(11, serverTime));

    await this.#broadcast(room, P.S_SYNC_PLAYBACK, P.concat(...parts), ws);
    await this.#persist();
  }

  async #onBufferReady(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member) return;
    const trackId = new P.Fields(payload).string(1);
    room.waitingFor.delete(member.userId);

    const host = room.members.get(room.hostId);
    if (!host || !host.ws) return;
    if (room.waitingFor.size) {
      const parts = [P.pbString(1, trackId)];
      for (const userId of [...room.waitingFor].sort()) parts.push(P.pbString(2, userId));
      await this.#send(host.ws, P.S_BUFFER_WAIT, P.concat(...parts));
    } else {
      await this.#broadcast(room, P.S_BUFFER_COMPLETE, P.pbString(1, trackId));
    }
  }

  async #onKickUser(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can kick');
      return;
    }
    const fields = new P.Fields(payload);
    const userId = fields.string(1);
    const reason = fields.string(2) || 'Removed by the host';
    const target = room.members.get(userId);
    if (!target) {
      await this.#sendError(ws, P.E_UNKNOWN_USER, 'no such user');
      return;
    }
    if (target.ws) {
      await this.#send(target.ws, P.S_KICKED, P.pbString(1, reason));
      target.ws.serializeAttachment(null);
    }
    room.members.delete(userId);
    this.sessions.delete(target.token);
    await this.#broadcast(room, P.S_USER_LEFT, P.concat(
      P.pbString(1, target.userId),
      P.pbString(2, target.username),
    ));
    await this.#persist();
  }

  async #onTransferHost(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can transfer');
      return;
    }
    const newHostId = new P.Fields(payload).string(1);
    const newHost = room.members.get(newHostId);
    if (!newHost) {
      await this.#sendError(ws, P.E_UNKNOWN_USER, 'no such user');
      return;
    }
    for (const current of room.members.values()) {
      current.isHost = current.userId === newHostId;
      if (current.ws) this.#bind(current.ws, current);
    }
    room.hostId = newHostId;
    room.revision += 1;
    await this.#broadcast(room, P.S_HOST_CHANGED, P.concat(
      P.pbString(1, newHost.userId),
      P.pbString(2, newHost.username),
    ));
    await this.#persist();
  }

  async #onRequestSync(ws) {
    const room = this.#room(ws);
    if (!room) {
      await this.#sendError(ws, P.E_NOT_IN_ROOM, 'not in a room');
      return;
    }
    await this.#send(ws, P.S_SYNC_STATE, room.syncStateMessage());
  }

  async #onReconnect(ws, payload) {
    const token = new P.Fields(payload).string(1);
    const session = token ? this.sessions.get(token) : null;
    if (!session) {
      await this.#sendError(ws, P.E_SESSION_NOT_FOUND, 'session expired');
      return;
    }
    const room = this.rooms.get(session.roomCode);
    const member = room && room.members.get(session.userId);
    if (!room || !member) {
      this.sessions.delete(token);
      await this.#sendError(ws, P.E_SESSION_NOT_FOUND, 'session expired');
      return;
    }

    member.connected = true;
    member.disconnectedAt = null;
    member.ws = ws;
    room.emptySince = null;
    this.#bind(ws, member);

    await this.#send(ws, P.S_RECONNECTED, P.concat(
      P.pbString(1, room.code),
      P.pbString(2, member.userId),
      P.pbMessage(3, room.stateMessage()),
      P.pbBool(4, member.isHost),
    ));
    await this.#broadcast(room, P.S_USER_RECONNECTED, P.concat(
      P.pbString(1, member.userId),
      P.pbString(2, member.username),
    ), ws);
    await this.#persist();
  }

  async #onSuggestTrack(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member) {
      await this.#sendError(ws, P.E_NOT_IN_ROOM, 'not in a room');
      return;
    }
    const raw = new P.Fields(payload).raw(1);
    if (!raw) {
      await this.#sendError(ws, P.E_INVALID_MESSAGE, 'missing track');
      return;
    }
    const track = P.Track.parse(raw);
    const suggestionId = P.randomUserId().slice(0, 10);
    room.suggestions.set(suggestionId, { userId: member.userId, track });

    const host = room.members.get(room.hostId);
    if (host && host.ws) {
      await this.#send(host.ws, P.S_SUGGESTION_RECEIVED, P.concat(
        P.pbString(1, suggestionId),
        P.pbString(2, member.userId),
        P.pbString(3, member.username),
        P.pbMessage(4, track.encode()),
      ));
    }
    await this.#persist();
  }

  async #onApproveSuggestion(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can approve');
      return;
    }
    const suggestionId = new P.Fields(payload).string(1);
    const entry = room.suggestions.get(suggestionId);
    if (!entry) {
      await this.#sendError(ws, P.E_UNKNOWN_USER, 'no such suggestion');
      return;
    }
    room.suggestions.delete(suggestionId);
    room.queue.push(entry.track);
    room.revision += 1;
    await this.#broadcast(room, P.S_SUGGESTION_APPROVED, P.concat(
      P.pbString(1, suggestionId),
      P.pbMessage(2, entry.track.encode()),
    ));
    await this.#persist();
  }

  async #onRejectSuggestion(ws, payload) {
    const room = this.#room(ws);
    const member = this.#member(ws);
    if (!room || !member || !member.isHost) {
      await this.#sendError(ws, P.E_NOT_HOST, 'only the host can reject');
      return;
    }
    const fields = new P.Fields(payload);
    const suggestionId = fields.string(1);
    const reason = fields.string(2) || 'Declined';
    const entry = room.suggestions.get(suggestionId);
    if (!entry) return;
    room.suggestions.delete(suggestionId);
    const owner = room.members.get(entry.userId);
    if (owner && owner.ws) {
      await this.#send(owner.ws, P.S_SUGGESTION_REJECTED, P.concat(
        P.pbString(1, suggestionId),
        P.pbString(2, reason),
      ));
    }
    await this.#persist();
  }
}
