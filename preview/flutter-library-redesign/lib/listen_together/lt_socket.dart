/// A binary WebSocket, the only transport Listen Together uses.
///
/// `package:web` rather than `dart:html`: the prototype is web-only, and this
/// keeps it off the deprecated library. Everything above this file talks in
/// `Uint8List`, so the transport can be swapped for `dart:io`'s `WebSocket`
/// when the client moves into the app.
library;

import 'dart:async';
import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

/// How a socket ended, so the UI can tell "the server hung up" from "we closed
/// it on purpose".
class LtClose {
  const LtClose({required this.code, required this.reason, required this.clean});

  final int code;
  final String reason;

  /// True when the local side initiated the close.
  final bool clean;
}

class LtSocketException implements Exception {
  const LtSocketException(this.message);

  final String message;

  @override
  String toString() => message;
}

class LtSocket {
  LtSocket(this.url);

  final String url;

  web.WebSocket? _socket;
  bool _closing = false;
  bool get closing => _closing;

  final StreamController<Uint8List> _messages =
      StreamController<Uint8List>.broadcast();
  final StreamController<LtClose> _closes =
      StreamController<LtClose>.broadcast();

  Stream<Uint8List> get messages => _messages.stream;
  Stream<LtClose> get closes => _closes.stream;

  /// Resolves once the handshake completes; rejects on error or timeout, in
  /// which case the caller gets a sentence rather than a raw DOM event.
  Future<void> open({Duration timeout = const Duration(seconds: 8)}) {
    final Completer<void> opened = Completer<void>();
    try {
      final web.WebSocket socket = web.WebSocket(url);
      socket.binaryType = 'arraybuffer';
      _socket = socket;

      socket.onopen = ((web.Event _) {
        if (!opened.isCompleted) opened.complete();
      }).toJS;

      socket.onerror = ((web.Event _) {
        if (!opened.isCompleted) {
          opened.completeError(
            LtSocketException('Could not reach ${Uri.parse(url).host}'),
          );
        }
      }).toJS;

      socket.onclose = ((web.CloseEvent event) {
        if (!opened.isCompleted) {
          opened.completeError(
            LtSocketException(
              'The server refused the connection (${event.code})',
            ),
          );
        }
        _closes.add(LtClose(
          code: event.code,
          reason: event.reason,
          clean: _closing,
        ));
      }).toJS;

      socket.onmessage = ((web.MessageEvent event) {
        final Uint8List? bytes = _asBytes(event.data);
        if (bytes != null) _messages.add(bytes);
      }).toJS;
    } catch (error) {
      throw LtSocketException('$error');
    }

    return opened.future.timeout(
      timeout,
      onTimeout: () {
        close();
        throw LtSocketException('No answer from ${Uri.parse(url).host}');
      },
    );
  }

  static Uint8List? _asBytes(JSAny? data) {
    if (data == null) return null;
    if (data.isA<JSArrayBuffer>()) {
      return (data as JSArrayBuffer).toDart.asUint8List();
    }
    if (data.isA<JSUint8Array>()) {
      return (data as JSUint8Array).toDart;
    }
    return null;
  }

  void send(List<int> bytes) {
    final web.WebSocket? socket = _socket;
    if (socket == null || socket.readyState != web.WebSocket.OPEN) {
      throw const LtSocketException('The connection is closed');
    }
    socket.send(Uint8List.fromList(bytes).toJS);
  }

  void close() {
    _closing = true;
    _socket?.close();
    _dispose();
  }

  void _dispose() {
    if (!_messages.isClosed) _messages.close();
    if (!_closes.isClosed) _closes.close();
  }
}
