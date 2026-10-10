/// The redesigned Listen Together screen.
///
/// What was wrong with the app's version, and what this fixes:
///
/// * The room-code field was drawn **before** any mode was chosen, and it
///   behaved like a mode switch: typing 8 characters hid *Create room* and
///   revealed *Join room*, so the join action was invisible until you guessed
///   the rule, and creating a room asked you for a code you did not have.
///   Here the choice is explicit — two segments, *Create a room* / *Join with a
///   code* — and the code field only exists in join mode.
/// * Server codes are **6 characters** (`metroserver` checks `len == 6`), while
///   the button stayed disabled until you typed 8. The field is now six cells
///   with a live counter.
/// * `isCreatingRoom` was written on tap and never read, so the screen looked
///   idle while the request was in flight. The pending state is drawn from
///   [LtPhase], and every terminal answer clears it.
/// * A refusal was collected and dropped, so the sheet went quiet. Failures now
///   arrive as a sentence with the fix, including the settings path when the
///   problem is the server itself.
library;

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../glossy_chrome.dart';
import '../glossy_tokens.dart';
import 'lt_client.dart';
import 'models.dart';
import 'protocol.dart';
import 'servers.dart';

enum TogetherMode { create, join }

class TogetherScreen extends StatefulWidget {
  const TogetherScreen({
    super.key,
    required this.client,
    this.onNavigate,
    this.onOpenServerSettings,
  });

  final LtClient client;
  final ValueChanged<int>? onNavigate;

  /// Settings → Integrations → Listen Together, where the server address lives.
  final VoidCallback? onOpenServerSettings;

  @override
  State<TogetherScreen> createState() => _TogetherScreenState();
}

class _TogetherScreenState extends State<TogetherScreen> {
  late final TextEditingController _name =
      TextEditingController(text: widget.client.username);
  final TextEditingController _code = TextEditingController();

  TogetherMode _mode = TogetherMode.create;
  bool _showTraffic = false;
  bool _copied = false;
  Timer? _ticker;

  @override
  void initState() {
    super.initState();
    widget.client.addListener(_onClientChanged);
    // The playhead is interpolated from the host's snapshot, so the room's
    // progress bar keeps moving without asking the server again.
    _ticker = Timer.periodic(const Duration(seconds: 1), (Timer _) {
      if (mounted && widget.client.isInRoom) setState(() {});
    });
  }

  @override
  void dispose() {
    widget.client.removeListener(_onClientChanged);
    _ticker?.cancel();
    _name.dispose();
    _code.dispose();
    super.dispose();
  }

  void _onClientChanged() {
    if (mounted) setState(() {});
  }

  String get _name_ => _name.text.trim();

  bool get _canAct => _name_.isNotEmpty;

  String get _blockedReason {
    if (_name_.isEmpty) return 'Add a name so the room can list you.';
    if (_mode == TogetherMode.join && _code.text.length != 6) {
      return 'Enter the 6-character code from the host.';
    }
    return '';
  }

  void _start() {
    widget.client.configure(username: _name_);
    if (_mode == TogetherMode.create) {
      widget.client.createRoom();
    } else {
      widget.client.joinRoom(_code.text);
    }
  }

  Future<void> _copyCode(String code) async {
    await Clipboard.setData(ClipboardData(text: code));
    if (!mounted) return;
    setState(() => _copied = true);
    Timer(const Duration(seconds: 2), () {
      if (mounted) setState(() => _copied = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final LtClient client = widget.client;

    return Column(
      children: <Widget>[
        GlossyHeader(
          eyebrow: 'LISTEN TOGETHER',
          title: 'Listen Together',
          subtitle: _subtitle(client),
          actions: <Widget>[
            GlossyIconButton(
              icon: Icons.dns_rounded,
              tooltip: 'Room server',
              onTap: widget.onOpenServerSettings ?? () {},
            ),
          ],
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(
              GlossyDimens.screenPadding,
              6,
              GlossyDimens.screenPadding,
              GlossyDimens.section,
            ),
            physics: const ClampingScrollPhysics(),
            children: <Widget>[
              _ServerStrip(
                client: client,
                onChange: widget.onOpenServerSettings,
              ),
              if (client.notice != null) ...<Widget>[
                const SizedBox(height: GlossyDimens.related),
                _NoticeCard(
                  message: client.notice!,
                  onDismiss: client.dismissNotice,
                ),
              ],
              if (client.isInRoom)
                ..._roomView(context, client)
              else ...<Widget>[
                const SizedBox(height: GlossyDimens.related),
                _lobbyCard(context, client),
                if (client.isPending) ...<Widget>[
                  const SizedBox(height: GlossyDimens.related),
                  _pendingCard(client),
                ],
                if (client.failure != null) ...<Widget>[
                  const SizedBox(height: GlossyDimens.related),
                  _failureCard(client),
                ],
                const SizedBox(height: GlossyDimens.related),
                _TrafficPanel(
                  client: client,
                  expanded: _showTraffic,
                  onToggle: () => setState(() => _showTraffic = !_showTraffic),
                ),
              ],
            ],
          ),
        ),
        if (!client.isInRoom)
          const GlossyMiniPlayer(
            title: 'Karma Police',
            subtitle: 'Radiohead · OK Computer',
            progress: 0.38,
            playing: true,
          ),
        GlossyNavBar(
          activeIndex: 3,
          onSelect: (int index) => widget.onNavigate?.call(index),
        ),
      ],
    );
  }

  String _subtitle(LtClient client) {
    switch (client.phase) {
      case LtPhase.inRoom:
        return 'Room ${client.room?.code ?? ''} · ${client.room?.members.length ?? 0} '
            '${(client.room?.members.length ?? 0) == 1 ? 'listener' : 'listeners'}';
      case LtPhase.creating:
        return 'Asking the server for a room…';
      case LtPhase.joining:
        return 'Knocking on ${_code.text.toUpperCase()}…';
      case LtPhase.waitingApproval:
        return 'Waiting for the host to let you in';
      case LtPhase.failed:
        return 'That attempt did not land';
      case LtPhase.connecting:
        return 'Connecting to ${client.hostLabel}…';
      case LtPhase.offline:
        return 'Host a room or join with a code';
    }
  }

  // -- lobby ---------------------------------------------------------------

  Widget _lobbyCard(BuildContext context, LtClient client) {
    final GlossyTokens t = context.glossy;
    final bool busy = client.phase == LtPhase.creating || client.phase == LtPhase.joining;
    return GlossyCard(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Who are you in the room?',
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: t.textPrimary,
            ),
          ),
          const SizedBox(height: 10),
          _NameField(
            controller: _name,
            onChanged: (String _) => setState(() {}),
          ),

          const SizedBox(height: GlossyDimens.section),
          _ModeToggle(
            mode: _mode,
            onChanged: (TogetherMode mode) => setState(() => _mode = mode),
          ),
          const SizedBox(height: GlossyDimens.related),

          // The code field belongs to one mode only. This is the bug that made
          // "Create room" look like it wanted a room code.
          if (_mode == TogetherMode.create)
            Text(
              'A 6-character code is generated and copied to your clipboard. '
              'Everyone who has it lands in your room.',
              style: TextStyle(fontSize: 11.5, height: 1.45, color: t.textLow),
            )
          else
            _CodeField(
              controller: _code,
              onChanged: (String _) => setState(() {}),
            ),

          const SizedBox(height: GlossyDimens.section),
          GlossyButton(
            label: _mode == TogetherMode.create ? 'Create room' : 'Join room',
            icon: _mode == TogetherMode.create
                ? Icons.add_rounded
                : Icons.login_rounded,
            busy: busy,
            onPressed: (_canAct && _blockedReason.isEmpty) ? _start : null,
          ),
          if (_blockedReason.isNotEmpty) ...<Widget>[
            const SizedBox(height: 8),
            Text(
              _blockedReason,
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 11, color: t.textMuted),
            ),
          ],
        ],
      ),
    );
  }

  Widget _pendingCard(LtClient client) {
    final GlossyTokens t = context.glossy;
    final bool atTheDoor = client.phase == LtPhase.waitingApproval;
    final String label = switch (client.phase) {
      LtPhase.creating => 'Creating the room…',
      LtPhase.joining => 'Sending ${_code.text.toUpperCase()} to the host…',
      LtPhase.waitingApproval => 'Waiting for host permission',
      LtPhase.connecting => 'Connecting to ${client.hostLabel}…',
      _ => 'Working…',
    };
    return GlossyCard(
      accent: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2, color: t.accent),
              ),
              const SizedBox(width: GlossyDimens.related),
              Expanded(
                child: Text(
                  label,
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            atTheDoor
                ? 'The host has a request card in their room and taps Let in. '
                    'This stays here until they decide — nothing is wrong while '
                    'it waits.'
                : 'A refusal (no such room, room full) comes back at once, so a '
                    'silent server turns into the reason instead of spinning forever.',
            style: TextStyle(fontSize: 11, height: 1.4, color: t.textLow),
          ),
          const SizedBox(height: GlossyDimens.related),
          GlossyButton(
            label: 'Cancel',
            tone: GlossyButtonTone.quiet,
            onPressed: client.cancelPending,
          ),
        ],
      ),
    );
  }

  Widget _failureCard(LtClient client) {
    final LtFailure failure = client.failure!;
    final GlossyTokens t = context.glossy;
    final Color danger = Theme.of(context).colorScheme.error;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: danger.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
        border: Border.all(
          color: danger.withValues(alpha: 0.35),
          width: GlossyDimens.hairline,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Icon(Icons.error_outline_rounded, size: 18, color: danger),
              const SizedBox(width: GlossyDimens.related),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      failure.message,
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        height: 1.35,
                        color: t.textPrimary,
                      ),
                    ),
                    if (failure.hint != null) ...<Widget>[
                      const SizedBox(height: 5),
                      Text(
                        failure.hint!,
                        style: TextStyle(fontSize: 11, height: 1.4, color: t.textSecondary),
                      ),
                    ],
                    if (failure.serverCode) ...<Widget>[
                      const SizedBox(height: 5),
                      Text(
                        'server said: ${failure.code}',
                        style: TextStyle(
                          fontSize: 10,
                          fontFamily: 'monospace',
                          color: t.textMuted,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: GlossyDimens.related),
          Row(
            children: <Widget>[
              Expanded(
                child: GlossyButton(
                  label: 'Try again',
                  tone: GlossyButtonTone.quiet,
                  onPressed: () {
                    client.dismissFailure();
                    _start();
                  },
                ),
              ),
              if (failure.hint != null && widget.onOpenServerSettings != null) ...<Widget>[
                const SizedBox(width: 10),
                Expanded(
                  child: GlossyButton(
                    label: 'Server settings',
                    tone: GlossyButtonTone.quiet,
                    onPressed: widget.onOpenServerSettings,
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  // -- room ----------------------------------------------------------------

  List<Widget> _roomView(BuildContext context, LtClient client) {
    final LtRoom room = client.room!;
    final GlossyTokens t = context.glossy;
    final bool isHost = client.isHost;
    // The server keeps an empty seat for a while, so a guest can be sitting in
    // a room whose host has already closed the app. Say that instead of
    // showing a room with nobody in charge of it.
    final String hostName = room.host?.username ?? '';
    final bool hostAway = !isHost && room.host == null;
    final int now = DateTime.now().millisecondsSinceEpoch;
    final int playhead = room.playheadMs(now);
    final int duration = room.currentTrack?.durationMs ?? 0;
    final double progress = duration == 0 ? 0 : (playhead / duration).clamp(0.0, 1.0);

    return <Widget>[
      const SizedBox(height: GlossyDimens.related),
      if (hostAway) ...<Widget>[
        const _NoticeCard(
          message: 'The host\u2019s app is closed. The room stays open for a '
              'couple of minutes \u2014 leave and rejoin with this code, or start '
              'your own room.',
          tone: _NoticeTone.warn,
        ),
        const SizedBox(height: GlossyDimens.related),
      ],
      _RoomCodeCard(
        room: room,
        isHost: isHost,
        hostAway: hostAway,
        copied: _copied,
        onCopy: () => _copyCode(room.code),
      ),

      if (isHost && client.joinRequests.isNotEmpty) ...<Widget>[
        GlossyCaption('Knocking · ${client.joinRequests.length}'),
        for (final LtJoinRequest request in client.joinRequests)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: GlossyCard(
              accent: true,
              child: Row(
                children: <Widget>[
                  GlossyCover(
                    seed: request.username,
                    size: 38,
                    circle: true,
                    radius: 19,
                  ),
                  const SizedBox(width: GlossyDimens.related),
                  Expanded(
                    child: Text(
                      '${request.username} wants in',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: t.textPrimary,
                      ),
                    ),
                  ),
                  _MiniAction(
                    label: 'Let in',
                    onTap: () => client.approveJoin(request.userId),
                  ),
                  const SizedBox(width: 8),
                  _MiniAction(
                    label: 'Decline',
                    quiet: true,
                    onTap: () => client.rejectJoin(request.userId),
                  ),
                ],
              ),
            ),
          ),
      ],

      GlossyCaption('In the room · ${room.members.length}'),
      for (final LtMember member in room.members)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: _MemberRow(
            member: member,
            isYou: member.userId == client.userId,
            canManage: isHost && member.userId != client.userId,
            onTap: () => _memberSheet(client, member),
          ),
        ),

      const GlossyCaption('Now playing'),
      _NowPlayingCard(
        client: client,
        room: room,
        isHost: isHost,
        progress: progress,
        playheadMs: playhead,
        durationMs: duration,
        label: isHost
            ? 'Your player drives the room'
            : (hostAway ? 'Waiting for the host to come back' : 'Following $hostName'),
      ),

      if (room.queue.isNotEmpty) ...<Widget>[
        GlossyCaption('Up next · ${room.queue.length}'),
        for (int i = 0; i < room.queue.length && i < 4; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Row(
              children: <Widget>[
                SizedBox(
                  width: 18,
                  child: Text(
                    '${i + 1}',
                    style: TextStyle(fontSize: 11, color: t.textMuted),
                  ),
                ),
                GlossyCover(seed: room.queue[i].title, size: 34, radius: 9),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        room.queue[i].title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w500,
                          color: t.textPrimary,
                        ),
                      ),
                      Text(
                        room.queue[i].subtitle,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 11, color: t.textLow),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
      ],

      const SizedBox(height: GlossyDimens.section),
      GlossyButton(
        label: 'Leave room',
        icon: Icons.logout_rounded,
        tone: GlossyButtonTone.danger,
        onPressed: client.leaveRoom,
      ),
      const SizedBox(height: GlossyDimens.related),
      _TrafficPanel(
        client: client,
        expanded: _showTraffic,
        onToggle: () => setState(() => _showTraffic = !_showTraffic),
      ),
    ];
  }

  void _memberSheet(LtClient client, LtMember member) {
    if (!client.isHost || member.userId == client.userId) return;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: context.glossy.player,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(GlossyDimens.cornerCard + 8),
        ),
      ),
      builder: (BuildContext sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(Icons.swap_horiz_rounded),
              title: Text('Make ${member.username} the host'),
              onTap: () {
                client.transferHost(member.userId);
                Navigator.of(sheetContext).pop();
              },
            ),
            ListTile(
              leading: const Icon(Icons.person_remove_rounded),
              title: Text('Remove ${member.username}'),
              onTap: () {
                client.kickUser(member.userId);
                Navigator.of(sheetContext).pop();
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------

class _ServerStrip extends StatelessWidget {
  const _ServerStrip({required this.client, this.onChange});

  final LtClient client;
  final VoidCallback? onChange;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final bool healthy = client.phase != LtPhase.failed;
    final Color dot = healthy ? t.accent : Theme.of(context).colorScheme.error;
    final String detail = client.latencyMs != null
        ? '${client.latencyMs} ms · ${client.serverVersion ?? 'unknown build'}'
        : 'not connected yet';

    return Row(
      children: <Widget>[
        Container(
          width: 7,
          height: 7,
          decoration: BoxDecoration(color: dot, shape: BoxShape.circle),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            '${serverDisplayName(client.serverUrl)} · $detail',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 11, color: t.textLow),
          ),
        ),
        if (onChange != null)
          GestureDetector(
            onTap: onChange,
            child: Text(
              'Change',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: t.accent,
              ),
            ),
          ),
      ],
    );
  }
}

class _ModeToggle extends StatelessWidget {
  const _ModeToggle({required this.mode, required this.onChanged});

  final TogetherMode mode;
  final ValueChanged<TogetherMode> onChanged;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: t.chip,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
        border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
      ),
      child: Row(
        children: <Widget>[
          Expanded(
            child: _segment(
              context,
              label: 'Create a room',
              icon: Icons.add_rounded,
              value: TogetherMode.create,
            ),
          ),
          Expanded(
            child: _segment(
              context,
              label: 'Join with a code',
              icon: Icons.login_rounded,
              value: TogetherMode.join,
            ),
          ),
        ],
      ),
    );
  }

  Widget _segment(
    BuildContext context, {
    required String label,
    required IconData icon,
    required TogetherMode value,
  }) {
    final GlossyTokens t = context.glossy;
    final bool active = mode == value;
    return GestureDetector(
      onTap: () => onChanged(value),
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        height: 38,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: active ? t.accent : null,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Icon(icon, size: 15, color: active ? t.onAccent : t.textSecondary),
            const SizedBox(width: 6),
            Flexible(
              child: Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w600,
                  color: active ? t.onAccent : t.textSecondary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _NameField extends StatelessWidget {
  const _NameField({required this.controller, required this.onChanged});

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final String initial = controller.text.trim().isEmpty
        ? '?'
        : controller.text.trim().characters.first.toUpperCase();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: t.chip,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
        border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
      ),
      child: Row(
        children: <Widget>[
          GlossyCover(seed: controller.text, size: 30, circle: true, radius: 15),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: controller,
              onChanged: onChanged,
              maxLength: 24,
              style: TextStyle(fontSize: 13.5, color: t.textPrimary),
              decoration: InputDecoration(
                border: InputBorder.none,
                isDense: true,
                counterText: '',
                hintText: 'Your name',
                hintStyle: TextStyle(fontSize: 13.5, color: t.textMuted),
              ),
            ),
          ),
          if (initial != '?')
            Text(
              initial,
              style: TextStyle(
                fontSize: 10,
                fontFamily: 'monospace',
                color: t.textMuted,
              ),
            ),
        ],
      ),
    );
  }
}

class _CodeField extends StatelessWidget {
  const _CodeField({required this.controller, required this.onChanged});

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: t.chip,
            borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
            border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
          ),
          child: Row(
            children: <Widget>[
              Icon(Icons.pin_rounded, size: 17, color: t.accent),
              const SizedBox(width: 10),
              Expanded(
                child: TextField(
                  controller: controller,
                  onChanged: (String value) {
                    final String upper = value.toUpperCase().replaceAll(
                          RegExp('[^A-Z0-9]'),
                          '',
                        );
                    if (upper != value) {
                      controller.value = TextEditingValue(
                        text: upper,
                        selection: TextSelection.collapsed(offset: upper.length),
                      );
                    }
                    onChanged(upper);
                  },
                  maxLength: 6,
                  textCapitalization: TextCapitalization.characters,
                  style: TextStyle(
                    fontSize: 17,
                    letterSpacing: 6,
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                  decoration: InputDecoration(
                    border: InputBorder.none,
                    isDense: true,
                    counterText: '',
                    hintText: 'ABC123',
                    hintStyle: TextStyle(
                      fontSize: 17,
                      letterSpacing: 6,
                      fontFamily: 'monospace',
                      color: t.textMuted,
                    ),
                  ),
                ),
              ),
              Text(
                '${controller.text.length}/6',
                style: TextStyle(fontSize: 10, fontFamily: 'monospace', color: t.textMuted),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _RoomCodeCard extends StatelessWidget {
  const _RoomCodeCard({
    required this.room,
    required this.isHost,
    required this.copied,
    required this.onCopy,
    this.hostAway = false,
  });

  final LtRoom room;
  final bool isHost;
  final bool copied;
  final VoidCallback onCopy;

  /// True while the host's seat is empty: the code is real, but nobody is
  /// driving.
  final bool hostAway;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final String hostName = hostAway
        ? '\u2014'
        : (room.host?.username ?? (isHost ? 'you' : 'the host'));
    return GlossyCard(
      accent: true,
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Icon(
                isHost ? Icons.wifi_tethering_rounded : Icons.people_alt_rounded,
                size: 17,
                color: t.accent,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  isHost
                      ? "You're the host"
                      : (hostAway
                          ? 'The host has left'
                          : 'Hosted by $hostName'),
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            'ROOM CODE',
            style: TextStyle(
              fontSize: 9.5,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.4,
              color: t.textMuted,
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  room.code,
                  style: TextStyle(
                    fontSize: 32,
                    height: 1.05,
                    letterSpacing: 8,
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                ),
              ),
              GestureDetector(
                onTap: onCopy,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                  decoration: BoxDecoration(
                    color: t.accentSoft,
                    borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
                  ),
                  child: Row(
                    children: <Widget>[
                      Icon(
                        copied ? Icons.check_rounded : Icons.copy_rounded,
                        size: 14,
                        color: t.accent,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        copied ? 'Copied' : 'Copy',
                        style: TextStyle(
                          fontSize: 11.5,
                          fontWeight: FontWeight.w600,
                          color: t.accent,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            isHost
                ? 'Anyone with this code can ask to join. You approve each one.'
                : 'Share the code with whoever is hosting the next room.',
            style: TextStyle(fontSize: 11, height: 1.4, color: t.textLow),
          ),
        ],
      ),
    );
  }
}

class _MemberRow extends StatelessWidget {
  const _MemberRow({
    required this.member,
    required this.isYou,
    required this.canManage,
    required this.onTap,
  });

  final LtMember member;
  final bool isYou;
  final bool canManage;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GlossyCard(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      onTap: canManage ? onTap : null,
      child: Row(
        children: <Widget>[
          Stack(
            children: <Widget>[
              Opacity(
                opacity: member.isConnected ? 1 : 0.45,
                child: GlossyCover(
                  seed: member.username,
                  size: 38,
                  circle: true,
                  radius: 19,
                ),
              ),
              Positioned(
                right: 0,
                bottom: 0,
                child: Container(
                  width: 11,
                  height: 11,
                  decoration: BoxDecoration(
                    color: member.isConnected ? t.accent : t.textMuted,
                    shape: BoxShape.circle,
                    border: Border.all(color: t.row, width: 2),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Flexible(
                      child: Text(
                        member.username,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w600,
                          color: t.textPrimary,
                        ),
                      ),
                    ),
                    if (isYou) ...<Widget>[
                      const SizedBox(width: 6),
                      _Chip(label: 'you', active: true),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  member.isHost
                      ? 'Host · controls playback'
                      : (member.isConnected ? 'Listener' : 'Away · seat held'),
                  style: TextStyle(fontSize: 11, color: t.textLow),
                ),
              ],
            ),
          ),
          if (member.isHost)
            Icon(Icons.workspace_premium_rounded, size: 17, color: t.accent),
          if (canManage) ...<Widget>[
            const SizedBox(width: 6),
            Icon(Icons.more_horiz_rounded, size: 18, color: t.textMuted),
          ],
        ],
      ),
    );
  }
}

class _NowPlayingCard extends StatelessWidget {
  const _NowPlayingCard({
    required this.client,
    required this.room,
    required this.isHost,
    required this.progress,
    required this.playheadMs,
    required this.durationMs,
    required this.label,
  });

  final LtClient client;
  final LtRoom room;
  final bool isHost;
  final double progress;
  final int playheadMs;
  final int durationMs;
  final String label;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final LtTrack? track = room.currentTrack;
    return GlossyCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              GlossyCover(
                seed: track?.title ?? 'Nothing yet',
                size: 52,
                radius: 14,
                icon: Icons.music_note_rounded,
              ),
              const SizedBox(width: GlossyDimens.related),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      track?.title ?? 'Nothing playing yet',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w600,
                        color: t.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      track?.subtitle.isNotEmpty == true
                          ? track!.subtitle
                          : 'The player fills this in as soon as the host plays.',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 11, color: t.textLow),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          ClipRRect(
            borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
            child: LinearProgressIndicator(
              value: progress == 0 ? 0.001 : progress,
              minHeight: 3,
              backgroundColor: t.chip,
              valueColor: AlwaysStoppedAnimation<Color>(t.accent),
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: <Widget>[
              Text(
                _clock(playheadMs),
                style: TextStyle(fontSize: 10.5, fontFamily: 'monospace', color: t.textLow),
              ),
              const Spacer(),
              Text(
                durationMs == 0 ? '--:--' : _clock(durationMs),
                style: TextStyle(fontSize: 10.5, fontFamily: 'monospace', color: t.textLow),
              ),
            ],
          ),
          const SizedBox(height: GlossyDimens.related),
          Row(
            children: <Widget>[
              Icon(Icons.graphic_eq_rounded, size: 15, color: t.accent),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 11, color: t.textSecondary),
                ),
              ),
              if (isHost) ...<Widget>[
                GlossyIconButton(
                  icon: Icons.skip_previous_rounded,
                  tooltip: 'Previous',
                  size: 32,
                  iconSize: 17,
                  onTap: () => client.playback(LtAction.skipPrev),
                ),
                const SizedBox(width: 8),
                GlossyIconButton(
                  icon: room.isPlaying ? Icons.pause_rounded : Icons.play_arrow_rounded,
                  tooltip: room.isPlaying ? 'Pause for everyone' : 'Play for everyone',
                  size: 32,
                  iconSize: 17,
                  onTap: () => client.playback(
                    room.isPlaying ? LtAction.pause : LtAction.play,
                    position: playheadMs,
                  ),
                ),
                const SizedBox(width: 8),
                GlossyIconButton(
                  icon: Icons.skip_next_rounded,
                  tooltip: 'Next',
                  size: 32,
                  iconSize: 17,
                  onTap: () => client.playback(LtAction.skipNext),
                ),
              ] else
                _MiniAction(
                  label: 'Re-sync',
                  quiet: true,
                  onTap: client.requestSync,
                ),
            ],
          ),
        ],
      ),
    );
  }

  static String _clock(int ms) {
    final int totalSeconds = ms ~/ 1000;
    final String minutes = (totalSeconds ~/ 60).toString();
    final String seconds = (totalSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }
}

/// What a notice is for: information, or a state the user should act on.
enum _NoticeTone { info, warn }

class _NoticeCard extends StatelessWidget {
  const _NoticeCard({
    required this.message,
    this.onDismiss,
    this.tone = _NoticeTone.info,
  });

  final String message;

  /// Absent when the notice describes a state rather than an event — the host
  /// being away is not something to dismiss.
  final VoidCallback? onDismiss;
  final _NoticeTone tone;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final bool warn = tone == _NoticeTone.warn;
    return GlossyCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(
            warn ? Icons.person_off_rounded : Icons.info_outline_rounded,
            size: 17,
            color: warn ? Theme.of(context).colorScheme.error : t.accent,
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Text(
              message,
              style: TextStyle(fontSize: 11.5, height: 1.4, color: t.textSecondary),
            ),
          ),
          if (onDismiss != null)
            GestureDetector(
              onTap: onDismiss,
              child: Icon(Icons.close_rounded, size: 16, color: t.textMuted),
            ),
        ],
      ),
    );
  }
}

class _TrafficPanel extends StatelessWidget {
  const _TrafficPanel({
    required this.client,
    required this.expanded,
    required this.onToggle,
  });

  final LtClient client;
  final bool expanded;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final List<LtLogLine> lines = client.log.reversed.take(expanded ? 14 : 3).toList();
    return GlossyCard(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                'WEBSOCKET',
                style: TextStyle(
                  fontSize: 9.5,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1.3,
                  color: t.textMuted,
                ),
              ),
              const Spacer(),
              GestureDetector(
                onTap: onToggle,
                child: Text(
                  expanded ? 'less' : 'more',
                  style: TextStyle(fontSize: 10.5, color: t.accent),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (lines.isEmpty)
            Text(
              'Messages appear here as they are sent and received.',
              style: TextStyle(fontSize: 10.5, color: t.textMuted),
            )
          else
            for (final LtLogLine line in lines)
              Padding(
                padding: const EdgeInsets.only(bottom: 3),
                child: Row(
                  children: <Widget>[
                    SizedBox(
                      width: 12,
                      child: Text(
                        line.direction,
                        style: TextStyle(
                          fontSize: 10.5,
                          fontFamily: 'monospace',
                          color: line.direction == '→' ? t.accent : t.textSecondary,
                        ),
                      ),
                    ),
                    Expanded(
                      child: Text(
                        line.text,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 10.5,
                          fontFamily: 'monospace',
                          color: t.textSecondary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.label, this.active = false});

  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: active ? t.accentSoft : t.chip,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 9.5,
          fontWeight: FontWeight.w600,
          color: active ? t.accent : t.textSecondary,
        ),
      ),
    );
  }
}

class _MiniAction extends StatelessWidget {
  const _MiniAction({required this.label, required this.onTap, this.quiet = false});

  final String label;
  final VoidCallback onTap;
  final bool quiet;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
        decoration: BoxDecoration(
          color: quiet ? t.chip : t.accent,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
          border: quiet
              ? Border.all(color: t.chipOutline, width: GlossyDimens.hairline)
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w600,
            color: quiet ? t.textSecondary : t.onAccent,
          ),
        ),
      ),
    );
  }
}
