/// Checks the two pieces of Listen Together that are pure logic: the protobuf
/// codec (does it produce the bytes the room server parses?) and the address
/// normalisation the settings screen depends on.
///
/// The client itself is exercised against the deployed Worker in the browser;
/// this file is what can be re-run without a network.
library;

import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:glossy_library_preview/listen_together/models.dart';
import 'package:glossy_library_preview/listen_together/protocol.dart';
import 'package:glossy_library_preview/listen_together/servers.dart';

void main() {
  group('addresses', () {
    test('a bare host and port grows a scheme and the room path', () {
      expect(normaliseServerUrl('192.168.1.24:8080'), 'ws://192.168.1.24:8080/ws');
    });

    test('a pasted https origin becomes wss and picks up /ws', () {
      expect(
        normaliseServerUrl('https://glossy-listen-together.izybro110.workers.dev'),
        'wss://glossy-listen-together.izybro110.workers.dev/ws',
      );
    });

    test('an address that is already complete is left alone', () {
      expect(
        normaliseServerUrl('wss://glossy-listen-together.izybro110.workers.dev/ws'),
        kGlossyCloudflareUrl,
      );
      expect(normaliseServerUrl('ws://10.0.0.5:8080/ws/'), 'ws://10.0.0.5:8080/ws');
    });

    test('the account label of a Cloudflare host never reaches the UI', () {
      expect(
        serverHostLabel(kGlossyCloudflareUrl),
        'glossy-listen-together.account.workers.dev',
      );
      expect(
        serverAddressLabel(kGlossyCloudflareUrl),
        'wss://glossy-listen-together.account.workers.dev/ws',
      );
      // The real hostname is only ever handed to the socket.
      expect(kGlossyCloudflareUrl, contains('izybro110'));
    });

    test('a host that is not a workers.dev subdomain is left alone', () {
      expect(maskAccountLabel('metroserverx.meowery.eu'),
          'metroserverx.meowery.eu');
      expect(serverHostLabel('wss://rx.meowery.eu/ws'), 'rx.meowery.eu');
      expect(serverSchemeLabel('https://example.com/ws'), 'wss');
    });

    test('known servers are named, unknown ones show their masked host', () {
      expect(serverDisplayName(kGlossyCloudflareUrl), 'Glossy on Cloudflare');
      expect(serverDisplayName('wss://somewhere.example/ws'),
          'somewhere.example');
    });
  });

  group('wire format', () {
    test('create_room carries the name in field 1', () {
      final (String type, PbMessage payload) = PbMessage.decodeEnvelope(
        envelope(Lt.createRoom, createRoomPayload('Aman')),
      );
      expect(type, 'create_room');
      expect(payload.string(1), 'Aman');
    });

    test('join_room carries the code in field 1 and the name in field 2', () {
      final (String type, PbMessage payload) = PbMessage.decodeEnvelope(
        envelope(Lt.joinRoom, joinRoomPayload('82V9SB', 'Riya')),
      );
      expect(type, 'join_room');
      expect(payload.string(1), '82V9SB');
      expect(payload.string(2), 'Riya');
    });

    test('the empty payloads really are empty', () {
      final (String type, PbMessage payload) =
          PbMessage.decodeEnvelope(envelope(Lt.leaveRoom, const <int>[]));
      expect(type, 'leave_room');
      expect(payload.has(1), isFalse);
    });

    test('room_created decodes with the server field numbers', () {
      final Uint8ListBuilder builder = Uint8ListBuilder()
        ..string(1, '82V9SB')
        ..string(2, 'f03d195f01c8')
        ..string(3, 'token-1');
      final (String type, PbMessage payload) = PbMessage.decodeEnvelope(
        envelope(Lt.roomCreated, builder.toBytes()),
      );
      expect(type, 'room_created');
      expect(payload.string(1), '82V9SB');
      expect(payload.string(2), 'f03d195f01c8');
      expect(payload.string(3), 'token-1');
    });

    test('a large timestamp survives the varint (web ints are 64-bit safe)',
        () {
      const int now = 1786000000000;
      final (String type, PbMessage payload) = PbMessage.decodeEnvelope(
        envelope(Lt.ping, pingPayload(now, 3)),
      );
      expect(type, 'ping');
      expect(payload.integer(1), now);
      expect(payload.integer(2), 3);
    });
  });

  group('compressed payloads', () {
    // Captured from the deployed Worker: it gzips `join_approved` (the payload
    // carries the whole room state) and sets `Envelope.compressed`. Without the
    // gunzip step every field reads as missing — which is exactly why a guest
    // landed in a room with no code, no host and no members.
    const String capturedJoinApproved =
        '0a0d6a6f696e5f617070726f766564127e1f8b08000000000002ffe362338972f1f63012e2494e'
        '343136364f33b14c344e9392303075f648cc480a09313737494b0ed675372b4cf337710d540ae3'
        '82eb30333536b4344935344f4903eae042e10bb138e626e649302a304a8971a1982dc412945999'
        'a8c068f177ffc2964926ae0c0c0df6018c00129484b6880000001801';

    test('join_approved inflates into the whole room', () {
      final (String type, PbMessage payload) =
          PbMessage.decodeEnvelope(_hex(capturedJoinApproved));
      expect(type, Lt.joinApproved);
      expect(payload.string(1), '4ZDKH2');
      expect(payload.string(2), isNotEmpty);

      final LtRoom room = LtRoom.fromPb(payload.message(4)!);
      expect(room.code, '4ZDKH2');
      expect(room.hostId, '653194e17dff');
      expect(
        room.members.map((LtMember member) => member.username),
        <String>['Aman', 'Riya'],
      );
      expect(room.host?.username, 'Aman');
      expect(room.members.where((LtMember member) => member.isHost).length, 1);
      expect(room.revision, 1);
    });

    test('an envelope that claims gzip but is not is still readable', () {
      final List<int> envelopeBytes = (PbWriter()
            ..string(1, Lt.roomCreated)
            ..message(2, createRoomPayload('Aman'))
            ..boolean(3, true))
          .toBytes();
      final (String type, PbMessage payload) =
          PbMessage.decodeEnvelope(envelopeBytes);
      expect(type, Lt.roomCreated);
      expect(payload.string(1), 'Aman');
    });

    test('a plain payload is read as-is when the flag is clear', () {
      final List<int> plain =
          envelope(Lt.joinRoom, joinRoomPayload('4ZDKH2', 'Riya'));
      expect(PbMessage.decodeEnvelope(plain).$2.string(1), '4ZDKH2');
    });

    test('a gzip flag over bytes that are not gzip decodes from the raw frame',
        () {
      // Defensive: a wrongly flagged frame must not throw inside the socket
      // handler and take the connection down with it.
      final List<int> mislabelled = (PbWriter()
            ..string(1, Lt.joinRoom)
            ..message(2, joinRoomPayload('4ZDKH2', 'Riya'))
            ..boolean(3, true))
          .toBytes();
      final (String type, PbMessage payload) =
          PbMessage.decodeEnvelope(mislabelled);
      expect(type, Lt.joinRoom);
      expect(payload.string(1), '4ZDKH2');
    });
  });

  group('room state', () {
    test('playhead advances with the wall clock while playing', () {
      const LtRoom room = LtRoom(
        code: 'ABC123',
        hostId: 'host',
        members: <LtMember>[],
        isPlaying: true,
        positionMs: 30000,
        lastUpdateMs: 1000,
      );
      expect(room.playheadMs(1000), 30000);
      expect(room.playheadMs(5000), 34000);
      expect(room.copyWith(isPlaying: false).playheadMs(5000), 30000);
    });

    test('every refusal the server can send has a sentence', () {
      for (final String code in <String>[
        LtError.roomNotFound,
        LtError.roomFull,
        LtError.roomLimit,
        LtError.notHost,
        LtError.notInRoom,
        LtError.unknownUser,
        LtError.sessionNotFound,
      ]) {
        final LtFailure failure =
            LtFailure.fromServer(code, 'the server explanation');
        expect(failure.code, code);
        expect(failure.message, isNotEmpty);
        expect(failure.message, isNot('the server explanation'));
      }
    });

    test('server problems point at the setting that fixes them', () {
      expect(LtFailure.fromServer(LtError.roomLimit, '').hint,
          contains('Settings → Integrations → Listen Together'));
    });

    test('a concrete reason replaces the server wording for known codes', () {
      expect(LtFailure.fromServer(LtError.roomNotFound, 'no room HUHUHU').message,
          'No room is using that code.');
      expect(
        LtFailure.fromServer(LtError.roomNotFound, 'no room HUHUHU').hint,
        contains('Check the 6 characters with the host'),
      );
      // Anything without its own sentence keeps the server's text, and only a
      // real server code is ever labelled as one.
      final LtFailure unknown = LtFailure.fromServer('weird_code', 'something');
      expect(unknown.message, 'something');
      expect(unknown.serverCode, isFalse);
    });
  });
}

/// Hex to bytes, for the fixtures captured from the deployed server.
Uint8List _hex(String value) => Uint8List.fromList(<int>[
      for (int i = 0; i < value.length; i += 2)
        int.parse(value.substring(i, i + 2), radix: 16),
    ]);

/// Tiny protobuf writer mirroring the server's encoders, used to build the
/// fixtures above. If these helpers and the client ever disagree, the tests
/// fail rather than the room server silently ignoring a field.
class Uint8ListBuilder {
  final List<int> _bytes = <int>[];

  void _varint(int value) {
    var v = value;
    while (v >= 128) {
      _bytes.add((v % 128) + 128);
      v = v ~/ 128;
    }
    _bytes.add(v);
  }

  void string(int field, String value) {
    _varint(field * 8 + 2);
    final List<int> encoded = value.codeUnits;
    _varint(encoded.length);
    _bytes.addAll(encoded);
  }

  List<int> toBytes() => _bytes;
}
