/// The Listen Together room client, ported to Dart.
///
/// One WebSocket, one room, and a phase the UI can draw directly:
///
/// ```
/// offline → connecting → creating / joining → waitingApproval → inRoom
///                    ↘ failed (carrying the sentence to show)
/// ```
///
/// The three behaviours the app was missing live here rather than in the screen:
///
/// * **Every terminal outcome lands somewhere.** `room_created`, `join_approved`,
///   `join_rejected`, a server `error`, a socket close and the watchdog all clear
///   the pending phase and either enter the room or produce a [LtFailure]. There
///   is no `else -> {}` that swallows an answer.
/// * **The watchdog is cancelled by the answer it is waiting for**, so one
///   refusal cannot be followed by a bogus "no answer" message.
/// * **A guest's mistake does not end the session.** An `error` while in a room
///   becomes a notice; only a failure while joining or creating is fatal.
library;

import 'dart:async';

import 'package:flutter/foundation.dart';

import 'lt_socket.dart';
import 'models.dart';
import 'protocol.dart';
import 'servers.dart';

enum LtPhase {
  /// No socket. Nothing is pending.
  offline,
  connecting,
  creating,
  joining,

  /// Joined the room, waiting at the host's door.
  waitingApproval,
  inRoom,
  failed,
}

class LtLogLine {
  const LtLogLine(this.direction, this.text);

  final String direction; // '→' sent, '←' received, '·' local
  final String text;
}

class LtClient extends ChangeNotifier {
  LtClient({this.serverUrl = kGlossyCloudflareUrl, this.username = ''});

  /// Long enough to survive a cold Durable Object waking up, short enough that
  /// a genuinely dead address turns into a sentence rather than a spinner.
  static const Duration _answerTimeout = Duration(seconds: 18);

  /// How long a join may go unanswered before it is treated as "at the door".
  /// The server answers a bad room code in a fraction of a second and says
  /// nothing else, so silence after this long is the host's decision pending.
  static const Duration _doorbellTimeout = Duration(seconds: 5);

  static const Duration _pingInterval = Duration(seconds: 20);

  LtSocket? _socket;
  Timer? _watchdog;
  Timer? _ping;
  int _sequence = 0;
  int _sentAtMs = 0;
  bool _disposed = false;

  /// Where the room lives, and the name the room lists you under. Both are
  /// written through [configure] once the user edits them.
  String serverUrl;
  String username;

  LtPhase _phase = LtPhase.offline;
  LtRoom? _room;
  String? _userId;
  LtFailure? _failure;
  String? _notice;
  String? _serverVersion;
  int? _latencyMs;
  final List<LtJoinRequest> _joinRequests = <LtJoinRequest>[];
  final List<LtLogLine> _log = <LtLogLine>[];

  // -- read-only state the screen draws ------------------------------------

  LtPhase get phase => _phase;
  LtRoom? get room => _room;
  String? get userId => _userId;
  LtFailure? get failure => _failure;
  String? get notice => _notice;
  String? get serverVersion => _serverVersion;
  int? get latencyMs => _latencyMs;
  List<LtJoinRequest> get joinRequests => List<LtJoinRequest>.unmodifiable(_joinRequests);
  List<LtLogLine> get log => List<LtLogLine>.unmodifiable(_log);

  bool get isInRoom => _phase == LtPhase.inRoom && _room != null;
  bool get isHost => isInRoom && _room!.hostId == _userId;
  bool get isPending =>
      _phase == LtPhase.connecting ||
      _phase == LtPhase.creating ||
      _phase == LtPhase.joining ||
      _phase == LtPhase.waitingApproval;

  String get hostLabel => serverHostLabel(serverUrl);

  // -- configuration -------------------------------------------------------

  /// Changing the address only takes effect on the next connect, so a live room
  /// is never yanked out from under the user by a settings edit.
  void configure({String? serverUrl, String? username}) {
    if (serverUrl != null && serverUrl.trim().isNotEmpty) {
      serverUrl = normaliseServerUrl(serverUrl);
    }
    if (username != null) username = username.trim();
    _notify();
  }

  // -- actions -------------------------------------------------------------

  Future<void> createRoom() async {
    _failure = null;
    _notice = null;
    _phase = LtPhase.creating;
    _notify();
    try {
      await _ensureSocket();
      _send(Lt.createRoom, createRoomPayload(username));
      _armWatchdog('The server accepted the connection but never answered.');
    } on LtSocketException catch (error) {
      _fail(LtError.unreachable, error.message);
    }
  }

  Future<void> joinRoom(String code) async {
    final String normalised = code.trim().toUpperCase();
    _failure = null;
    _notice = null;
    _phase = LtPhase.joining;
    _notify();
    try {
      await _ensureSocket();
      _send(Lt.joinRoom, joinRoomPayload(normalised, username));
      // A refusal (no such room, room full) comes back immediately; a room that
      // exists sends the guest nothing until the host decides. So this is a
      // doorbell, not a failure timer: silence means the request is standing.
      _armDoorbell();
    } on LtSocketException catch (error) {
      _fail(LtError.unreachable, error.message);
    }
  }

  void approveJoin(String userId) {
    _joinRequests.removeWhere((LtJoinRequest r) => r.userId == userId);
    _send(Lt.approveJoin, approveJoinPayload(userId));
    _notify();
  }

  void rejectJoin(String userId, [String reason = 'Host declined']) {
    _joinRequests.removeWhere((LtJoinRequest r) => r.userId == userId);
    _send(Lt.rejectJoin, rejectJoinPayload(userId, reason));
    _notify();
  }

  void transferHost(String userId) {
    _send(Lt.transferHost, transferHostPayload(userId));
  }

  void kickUser(String userId, [String reason = 'Removed by host']) {
    _send(Lt.kickUser, kickUserPayload(userId, reason));
  }

  /// Gives up on a pending create/join without tearing the socket down. A late
  /// answer is harmless: the server either created a room we then leave, or the
  /// request already failed.
  void cancelPending() {
    _watchdog?.cancel();
    if (_phase == LtPhase.creating ||
        _phase == LtPhase.joining ||
        _phase == LtPhase.connecting ||
        _phase == LtPhase.waitingApproval) {
      _phase = LtPhase.offline;
    }
    _failure = null;
    _notice = null;
    _logLine('·', 'Cancelled');
    _notify();
  }

  void playback(String action, {int? position}) {
    _send(Lt.playbackAction, playbackPayload(action, position: position));
  }

  void requestSync() => _send(Lt.requestSync, const <int>[]);

  void leaveRoom() {
    if (_phase == LtPhase.inRoom) _send(Lt.leaveRoom, const <int>[]);
    _watchdog?.cancel();
    _forgetRoom();
    _notice = null;
    _failure = null;
    _phase = LtPhase.offline;
    _notify();
  }

  /// Drops the local room state without touching the socket.
  void _forgetRoom() {
    _room = null;
    _joinRequests.clear();
  }

  /// Clears a failure card without touching the socket.
  void dismissFailure() {
    _failure = null;
    _notice = null;
    if (_phase == LtPhase.failed) _phase = LtPhase.offline;
    _notify();
  }

  void dismissNotice() {
    _notice = null;
    _notify();
  }

  void disconnect() {
    _watchdog?.cancel();
    _ping?.cancel();
    _socket?.close();
    _socket = null;
    _room = null;
    _joinRequests.clear();
    _serverVersion = null;
    _latencyMs = null;
    _phase = LtPhase.offline;
    _logLine('·', 'Disconnected');
    _notify();
  }

  @override
  void dispose() {
    _disposed = true;
    _watchdog?.cancel();
    _ping?.cancel();
    _socket?.close();
    super.dispose();
  }

  // -- plumbing ------------------------------------------------------------

  Future<void> _ensureSocket() async {
    final LtSocket? existing = _socket;
    if (existing != null && !existing.closing) return;

    _phase = LtPhase.connecting;
    _notify();
    _logLine('·', 'Opening ${serverAddressLabel(serverUrl)}');

    final LtSocket socket = LtSocket(normaliseServerUrl(serverUrl));
    _socket = socket;
    socket.messages.listen(_onMessage);
    socket.closes.listen(_onClose);
    await socket.open();

    _send(Lt.clientCapabilities,
        clientCapabilitiesPayload(clientVersion: 'glossy-flutter-0.1'));
    _sequence = 0;
    _sentAtMs = DateTime.now().millisecondsSinceEpoch;
    _send(Lt.ping, pingPayload(_sentAtMs, _sequence));
    _ping?.cancel();
    _ping = Timer.periodic(_pingInterval, (Timer _) {
      _sentAtMs = DateTime.now().millisecondsSinceEpoch;
      _send(Lt.ping, pingPayload(_sentAtMs, ++_sequence));
    });
    _logLine('·', 'Connected to ${serverHostLabel(serverUrl)}');
  }

  void _armWatchdog(String message) {
    _watchdog?.cancel();
    _watchdog = Timer(_answerTimeout, () {
      _fail(LtError.noAnswer, message);
    });
  }

  /// After [_doorbellTimeout] with no answer, the join is waiting on the host
  /// rather than failing: the phase moves to [LtPhase.waitingApproval] and the
  /// screen says so instead of reporting a timeout the user cannot act on.
  void _armDoorbell() {
    _watchdog?.cancel();
    _watchdog = Timer(_doorbellTimeout, () {
      if (_phase == LtPhase.joining) {
        _phase = LtPhase.waitingApproval;
        _logLine('·', 'Waiting for the host to let us in');
        _notify();
      }
    });
  }

  void _send(String type, List<int> payload) {
    final LtSocket? socket = _socket;
    if (socket == null || socket.closing) {
      _fail(LtError.unreachable, 'The connection to ${serverHostLabel(serverUrl)} is closed.');
      return;
    }
    try {
      socket.send(envelope(type, payload));
      _logLine('→', type);
    } on LtSocketException catch (error) {
      _fail(LtError.unreachable, error.message);
    }
  }

  void _onClose(LtClose close) {
    _watchdog?.cancel();
    _ping?.cancel();
    if (_disposed) return;
    _socket = null;
    _forgetRoom();
    _serverVersion = null;
    _latencyMs = null;
    _logLine('·', 'Socket closed (${close.code}${close.reason.isEmpty ? '' : ' ${close.reason}'})');
    if (close.clean) {
      _phase = LtPhase.offline;
    } else {
      _fail(
        LtError.unreachable,
        'The connection to ${serverHostLabel(serverUrl)} dropped.',
      );
    }
  }

  void _onMessage(Uint8List bytes) {
    final (String type, PbMessage payload) = PbMessage.decodeEnvelope(bytes);
    _logLine('←', type);
    switch (type) {
      case Lt.serverCapabilities:
        _serverVersion = payload.string(3, 'this server');
        _notify();
      case Lt.pong:
        _latencyMs = DateTime.now().millisecondsSinceEpoch - _sentAtMs;
        _notify();
      case Lt.roomCreated:
        _watchdog?.cancel();
        _userId = payload.string(2);
        _room = LtRoom(
          code: payload.string(1),
          hostId: payload.string(2),
          members: <LtMember>[
            LtMember(
              userId: payload.string(2),
              username: username.isEmpty ? 'Host' : username,
              isHost: true,
            ),
          ],
        );
        _phase = LtPhase.inRoom;
        _notify();
      case Lt.joinRequest:
        final LtJoinRequest request = LtJoinRequest(
          userId: payload.string(1),
          username: payload.string(2, 'Guest'),
        );
        _joinRequests
          ..removeWhere((LtJoinRequest r) => r.userId == request.userId)
          ..add(request);
        _notify();
      case Lt.joinApproved:
        _watchdog?.cancel();
        _userId = payload.string(2);
        _room = payload.has(4)
            ? LtRoom.fromPb(payload.message(4)!)
            : LtRoom(code: payload.string(1), hostId: '', members: <LtMember>[]);
        _phase = LtPhase.inRoom;
        _notify();
      case Lt.joinRejected:
        _watchdog?.cancel();
        _fail('join_rejected', payload.string(1, 'The host declined your request.'));
      case Lt.userJoined:
        _upsertMember(payload.string(1), payload.string(2, 'Guest'), isHost: false);
      case Lt.userLeft:
        _removeMember(payload.string(1));
      case Lt.userDisconnected:
        _setMemberConnected(payload.string(1), false);
      case Lt.userReconnected:
        _setMemberConnected(payload.string(1), true);
      case Lt.hostChanged:
        _room = _room?.copyWith(hostId: payload.string(1));
        _notify();
      case Lt.kicked:
        // The one failure that really does end the room.
        _forgetRoom();
        _fail('kicked', 'You were removed from the room: ${payload.string(1, 'no reason given')}');
      case Lt.syncState:
        _applySyncState(payload);
      case Lt.syncPlayback:
        _applyPlayback(payload);
      case Lt.error:
        _onServerError(payload.string(1), payload.string(2, 'The server reported an error.'));
      default:
        break;
    }
  }

  void _applySyncState(PbMessage payload) {
    final LtRoom? current = _room;
    if (current == null) return;
    _room = current.copyWith(
      currentTrack:
          payload.has(1) ? LtTrack.fromPb(payload.message(1)!) : current.currentTrack,
      isPlaying: payload.boolean(2),
      positionMs: payload.integer(3),
      lastUpdateMs: payload.integer(4),
      queue: payload.has(5)
          ? payload.messages(5).map(LtTrack.fromPb).toList()
          : current.queue,
      volume: payload.has(6) ? payload.decimal(6, current.volume) : current.volume,
      revision: payload.integer(7, current.revision),
      clearTrack: !payload.has(1),
    );
    _notify();
  }

  void _applyPlayback(PbMessage payload) {
    final LtRoom? current = _room;
    if (current == null) return;
    final String action = payload.string(1);
    final int position = payload.integer(3);
    switch (action) {
      case LtAction.play:
        _room = current.copyWith(
          isPlaying: true,
          positionMs: position,
          lastUpdateMs: DateTime.now().millisecondsSinceEpoch,
          currentTrack:
              payload.has(4) ? LtTrack.fromPb(payload.message(4)!) : current.currentTrack,
          revision: payload.integer(10, current.revision),
        );
      case LtAction.pause:
        _room = current.copyWith(isPlaying: false, positionMs: position);
      case LtAction.seek:
        _room = current.copyWith(
          positionMs: position,
          lastUpdateMs: DateTime.now().millisecondsSinceEpoch,
        );
      case LtAction.skipNext || LtAction.skipPrev:
        _room = current.copyWith(
          currentTrack:
              payload.has(4) ? LtTrack.fromPb(payload.message(4)!) : current.currentTrack,
          positionMs: 0,
          lastUpdateMs: DateTime.now().millisecondsSinceEpoch,
          isPlaying: payload.has(4) ? current.isPlaying : false,
          queue: current.queue.isEmpty
              ? current.queue
              : current.queue.sublist(1),
        );
      default:
        if (payload.has(4)) {
          _room = current.copyWith(currentTrack: LtTrack.fromPb(payload.message(4)!));
        }
    }
    _notify();
  }

  void _onServerError(String code, String message) {
    final LtFailure failure = LtFailure.fromServer(code, message);
    _fail(failure.code, failure.message, hint: failure.hint, serverCode: true);
  }

  void _upsertMember(String userId, String username, {required bool isHost}) {
    final LtRoom? current = _room;
    if (current == null) return;
    final List<LtMember> members = List<LtMember>.of(current.members);
    final int index = members.indexWhere((LtMember m) => m.userId == userId);
    final LtMember member = LtMember(
      userId: userId,
      username: username,
      isHost: isHost || (index >= 0 && members[index].isHost),
    );
    if (index >= 0) {
      members[index] = member;
    } else {
      members.add(member);
    }
    _room = current.copyWith(members: members);
    _notify();
  }

  void _removeMember(String userId) {
    final LtRoom? current = _room;
    if (current == null) return;
    _room = current.copyWith(
      members: current.members.where((LtMember m) => m.userId != userId).toList(),
    );
    _notify();
  }

  void _setMemberConnected(String userId, bool connected) {
    final LtRoom? current = _room;
    if (current == null) return;
    _room = current.copyWith(
      members: <LtMember>[
        for (final LtMember member in current.members)
          member.userId == userId ? member.copyWith(isConnected: connected) : member,
      ],
    );
    _notify();
  }

  void _fail(String code, String message, {String? hint, bool serverCode = false}) {
    _watchdog?.cancel();
    final LtFailure failure =
        LtFailure(code, message, hint: hint, serverCode: serverCode);

    // A failure must never hide a room that is currently open. A refused action
    // ("already in a room", "only the host can…") is a notice on top of the
    // room; only being removed, or losing the socket, closes it — and both of
    // those forget the room before they get here.
    if (isInRoom) {
      _notice = failure.hint == null
          ? failure.message
          : '${failure.message} ${failure.hint}';
      _logLine('·', 'Notice: $message');
      _notify();
      return;
    }

    _failure = failure;
    _phase = LtPhase.failed;
    _notice = null;
    _logLine('·', 'Failed: $message');
    _notify();
  }

  void _logLine(String direction, String text) {
    _log.add(LtLogLine(direction, text));
    if (_log.length > 40) _log.removeAt(0);
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  /// Opens a throwaway socket and asks the server to identify itself. Used by
  /// Settings → Listen Together → Test connection, which cannot call the health
  /// route from a browser: that route carries no CORS headers, while the
  /// WebSocket handshake does not need them.
  static Future<LtProbeResult> probe(String rawUrl) async {
    final String url = normaliseServerUrl(rawUrl);
    final Uri? uri = Uri.tryParse(url);
    if (uri == null || uri.host.isEmpty) {
      return const LtProbeResult(reachable: false, problem: 'That is not an address.');
    }

    final LtSocket socket = LtSocket(url);
    final Completer<LtProbeResult> answer = Completer<LtProbeResult>();
    final int sentAt = DateTime.now().millisecondsSinceEpoch;

    socket.messages.listen((Uint8List bytes) {
      final (String type, PbMessage payload) = PbMessage.decodeEnvelope(bytes);
      if (answer.isCompleted) return;
      if (type == Lt.serverCapabilities) {
        answer.complete(LtProbeResult(
          reachable: true,
          version: payload.string(3, 'unknown build'),
          latencyMs: DateTime.now().millisecondsSinceEpoch - sentAt,
        ));
      } else if (type == Lt.pong) {
        answer.complete(LtProbeResult(
          reachable: true,
          latencyMs: DateTime.now().millisecondsSinceEpoch - sentAt,
        ));
      } else if (type == Lt.error) {
        answer.complete(LtProbeResult(
          reachable: true,
          version: 'no capabilities',
          latencyMs: DateTime.now().millisecondsSinceEpoch - sentAt,
        ));
      }
    });
    socket.closes.listen((LtClose close) {
      if (!answer.isCompleted) {
        answer.complete(LtProbeResult(
          reachable: false,
          problem: 'The server closed the connection (${close.code}).',
        ));
      }
    });

    try {
      await socket.open(timeout: const Duration(seconds: 8));
      socket.send(envelope(
        Lt.clientCapabilities,
        clientCapabilitiesPayload(clientVersion: 'glossy-flutter-0.1'),
      ));
      socket.send(envelope(
        Lt.ping,
        pingPayload(DateTime.now().millisecondsSinceEpoch, 0),
      ));
      return await answer.future.timeout(
        const Duration(seconds: 8),
        onTimeout: () => const LtProbeResult(
          reachable: false,
          problem: 'Connected, but the server did not identify itself.',
        ),
      );
    } on LtSocketException catch (error) {
      return LtProbeResult(reachable: false, problem: error.message);
    } finally {
      socket.close();
    }
  }
}
