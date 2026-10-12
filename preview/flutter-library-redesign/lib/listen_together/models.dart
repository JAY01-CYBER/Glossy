/// The shapes the Listen Together screen draws: a room, its members, the track
/// that is playing, and the one sentence a failure turns into.
library;

import 'protocol.dart';

class LtTrack {
  const LtTrack({
    required this.id,
    required this.title,
    required this.artist,
    this.album,
    this.durationMs = 0,
    this.thumbnail,
  });

  factory LtTrack.fromPb(PbMessage pb) => LtTrack(
        id: pb.string(1),
        title: pb.string(2, 'Unknown track'),
        artist: pb.string(3),
        album: pb.string(4).isEmpty ? null : pb.string(4),
        durationMs: pb.integer(5),
        thumbnail: pb.string(6).isEmpty ? null : pb.string(6),
      );

  final String id;
  final String title;
  final String artist;
  final String? album;
  final int durationMs;
  final String? thumbnail;

  String get subtitle =>
      <String>[if (artist.isNotEmpty) artist, ?album].join(' · ');
}

class LtMember {
  const LtMember({
    required this.userId,
    required this.username,
    this.isHost = false,
    this.isConnected = true,
  });

  factory LtMember.fromPb(PbMessage pb) => LtMember(
        userId: pb.string(1),
        username: pb.string(2, 'Guest'),
        isHost: pb.boolean(3),
        isConnected: pb.boolean(4, true),
      );

  final String userId;
  final String username;
  final bool isHost;

  /// False while the member is inside their session expiry window — the server
  /// keeps their seat and their queue slot for a while.
  final bool isConnected;

  LtMember copyWith({bool? isHost, bool? isConnected}) => LtMember(
        userId: userId,
        username: username,
        isHost: isHost ?? this.isHost,
        isConnected: isConnected ?? this.isConnected,
      );
}

class LtRoom {
  const LtRoom({
    required this.code,
    required this.hostId,
    required this.members,
    this.currentTrack,
    this.isPlaying = false,
    this.positionMs = 0,
    this.lastUpdateMs = 0,
    this.volume = 1,
    this.queue = const <LtTrack>[],
    this.revision = 0,
  });

  factory LtRoom.fromPb(PbMessage pb) => LtRoom(
        code: pb.string(1),
        hostId: pb.string(2),
        members: pb.messages(3).map(LtMember.fromPb).toList(),
        currentTrack: pb.has(4) ? LtTrack.fromPb(pb.message(4)!) : null,
        isPlaying: pb.boolean(5),
        positionMs: pb.integer(6),
        lastUpdateMs: pb.integer(7),
        volume: pb.decimal(8, 1),
        queue: pb.messages(9).map(LtTrack.fromPb).toList(),
        revision: pb.integer(10),
      );

  final String code;
  final String hostId;
  final List<LtMember> members;
  final LtTrack? currentTrack;
  final bool isPlaying;
  final int positionMs;
  final int lastUpdateMs;
  final double volume;
  final List<LtTrack> queue;
  final int revision;

  LtMember? get host {
    for (final LtMember member in members) {
      if (member.userId == hostId) return member;
    }
    return null;
  }

  /// Where the host's playhead is now: the snapshot position plus however long
  /// the room has been playing since.
  int playheadMs(int nowMs) {
    if (!isPlaying || lastUpdateMs == 0) return positionMs;
    return positionMs + (nowMs - lastUpdateMs).clamp(0, 3600000);
  }

  LtRoom copyWith({
    String? code,
    String? hostId,
    List<LtMember>? members,
    LtTrack? currentTrack,
    bool? isPlaying,
    int? positionMs,
    int? lastUpdateMs,
    double? volume,
    List<LtTrack>? queue,
    int? revision,
    bool clearTrack = false,
  }) =>
      LtRoom(
        code: code ?? this.code,
        hostId: hostId ?? this.hostId,
        members: members ?? this.members,
        currentTrack: clearTrack ? null : (currentTrack ?? this.currentTrack),
        isPlaying: isPlaying ?? this.isPlaying,
        positionMs: positionMs ?? this.positionMs,
        lastUpdateMs: lastUpdateMs ?? this.lastUpdateMs,
        volume: volume ?? this.volume,
        queue: queue ?? this.queue,
        revision: revision ?? this.revision,
      );
}

/// A guest waiting at the door, shown to the host as an approval card.
class LtJoinRequest {
  const LtJoinRequest({required this.userId, required this.username});

  final String userId;
  final String username;
}

/// A failure with the sentence the user reads. `hint` is set whenever the fix
/// is a different server address, so the UI can point at the setting by name.
class LtFailure {
  const LtFailure(
    this.code,
    this.message, {
    this.hint,
    this.serverCode = false,
  });

  final String code;
  final String message;
  final String? hint;

  /// True when the code came from the server's `error` payload. A timeout or a
  /// dead socket is the client's own diagnosis, and showing "server said:
  /// no_answer" for something the server never said reads as a server fault.
  final bool serverCode;

  static LtFailure fromServer(String code, String serverMessage) {
    switch (code) {
      case LtError.roomNotFound:
        return const LtFailure(
          LtError.roomNotFound,
          'No room is using that code.',
          hint: 'Check the 6 characters with the host — codes expire when the room empties.',
        );
      case LtError.roomFull:
        return const LtFailure(
          LtError.roomFull,
          'That room is already full.',
        );
      case LtError.roomLimit:
        return const LtFailure(
          LtError.roomLimit,
          'This server is at capacity and is not taking new rooms.',
          hint: 'Pick another server under Settings → Integrations → Listen Together, or host your own.',
        );
      case LtError.notHost:
        return const LtFailure(
          LtError.notHost,
          'Only the host can do that.',
        );
      case LtError.notInRoom:
        return const LtFailure(
          LtError.notInRoom,
          'You are no longer in a room.',
          hint: 'Join again with the room code from the host.',
        );
      case LtError.unknownUser:
        return const LtFailure(
          LtError.unknownUser,
          'That request is no longer waiting.',
        );
      case LtError.sessionNotFound:
        return const LtFailure(
          LtError.sessionNotFound,
          'Your session expired while you were away.',
          hint: 'Join again with the room code — the host keeps the room open.',
        );
      default:
        return LtFailure(code, serverMessage);
    }
  }
}

/// Hosts the answer from the settings screen's "Test connection" button.
class LtProbeResult {
  const LtProbeResult({
    required this.reachable,
    this.version,
    this.latencyMs,
    this.problem,
  });

  final bool reachable;
  final String? version;
  final int? latencyMs;
  final String? problem;
}
