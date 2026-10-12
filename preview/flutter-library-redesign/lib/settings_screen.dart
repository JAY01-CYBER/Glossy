/// Settings, redesigned in Flutter.
///
/// The specific hole this fills: the app's Listen Together settings screen owns
/// the room server address, but there is no Cloudflare entry anywhere in the
/// shipped list — the registry seeds Metrolist/The Meowery and refreshes from
/// Echo Music's `server.json`, so the Worker deployment in `metroserver-worker/`
/// can only be reached by pasting a URL nobody has. Here the deployment is the
/// first preset, the address is a first-class field, and "Test connection"
/// proves it answers without needing the health route (which carries no CORS
/// headers and therefore cannot be fetched from a browser).
library;

import 'package:flutter/material.dart';

import 'glossy_chrome.dart';
import 'glossy_tokens.dart';
import 'listen_together/lt_client.dart';
import 'listen_together/models.dart';
import 'listen_together/servers.dart';

enum SettingsPage { hub, listenTogether }

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    super.key,
    required this.client,
    required this.mode,
    required this.onModeChanged,
    this.onNavigate,
    this.initialPage = SettingsPage.hub,
  });

  final LtClient client;
  final ThemeMode mode;
  final ValueChanged<ThemeMode> onModeChanged;
  final ValueChanged<int>? onNavigate;
  final SettingsPage initialPage;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late SettingsPage _page = widget.initialPage;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        if (_page == SettingsPage.hub)
          const GlossyHeader(
            eyebrow: 'SETTINGS',
            title: 'Settings',
            subtitle: 'Integrations carry the room server',
          )
        else
          _DetailHeader(onBack: () => setState(() => _page = SettingsPage.hub)),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(
              GlossyDimens.screenPadding,
              6,
              GlossyDimens.screenPadding,
              GlossyDimens.section,
            ),
            physics: const ClampingScrollPhysics(),
            children: _page == SettingsPage.hub
                ? _hub(context)
                : _listenTogetherPage(context),
          ),
        ),
        GlossyNavBar(
          activeIndex: 4,
          onSelect: (int index) => widget.onNavigate?.call(index),
        ),
      ],
    );
  }

  // -- hub -----------------------------------------------------------------

  List<Widget> _hub(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final LtClient client = widget.client;

    return <Widget>[
      const GlossyCaption('Integrations'),
      GlossyCard(
        padding: EdgeInsets.zero,
        onTap: () => setState(() => _page = SettingsPage.listenTogether),
        child: Column(
          children: <Widget>[
            _SettingRow(
              icon: Icons.people_alt_rounded,
              title: 'Listen Together',
              subtitle: 'Server: ${serverDisplayName(client.serverUrl)}',
              trailing: Icon(Icons.chevron_right_rounded, size: 20, color: t.textMuted),
            ),
            const _RowDivider(),
            _SettingRow(
              icon: Icons.music_note_rounded,
              title: 'Last.fm',
              subtitle: 'Scrobbling · not part of this prototype',
              trailing: _Chip(label: 'as-is'),
            ),
            const _RowDivider(),
            _SettingRow(
              icon: Icons.sync_rounded,
              title: 'Discord presence',
              subtitle: 'Not part of this prototype',
              trailing: _Chip(label: 'as-is'),
            ),
          ],
        ),
      ),
      const GlossyCaption('Appearance'),
      GlossyCard(
        padding: EdgeInsets.zero,
        child: Column(
          children: <Widget>[
            _SettingRow(
              icon: Icons.contrast_rounded,
              title: 'Theme',
              subtitle: widget.mode == ThemeMode.dark ? 'Dark' : 'Light',
              trailing: _ThemeSwitch(
                mode: widget.mode,
                onChanged: widget.onModeChanged,
              ),
            ),
            const _RowDivider(),
            _SettingRow(
              icon: Icons.palette_rounded,
              title: 'Dynamic colour',
              subtitle: 'Glossy token palette in this prototype',
              trailing: _Chip(label: 'tokens'),
            ),
          ],
        ),
      ),
      const GlossyCaption('Room hosting'),
      GlossyCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(
              'One Worker, one Durable Object',
              style: TextStyle(
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
                color: t.textPrimary,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'The address below is the server everyone meets at. It is the '
              'Worker deployment committed in metroserver-worker/: a Worker '
              'front door plus one hibernating Durable Object that owns every '
              'room, free plan, no paid tier.',
              style: TextStyle(fontSize: 11.5, height: 1.45, color: t.textSecondary),
            ),
            const SizedBox(height: GlossyDimens.related),
            Row(
              children: <Widget>[
                Expanded(
                  child: GlossyButton(
                    label: 'Listen Together settings',
                    tone: GlossyButtonTone.quiet,
                    icon: Icons.dns_rounded,
                    onPressed: () => setState(() => _page = SettingsPage.listenTogether),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    ];
  }

  // -- Listen Together detail ----------------------------------------------

  List<Widget> _listenTogetherPage(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return <Widget>[
      _ListenTogetherServerCard(client: widget.client),
      const GlossyCaption('Identity'),
      _IdentityCard(client: widget.client),
      const GlossyCaption('Known servers'),
      for (final LtServerPreset preset in kLtServerPresets)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: GlossyCard(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            onTap: () => widget.client.configure(serverUrl: preset.url),
            child: Row(
              children: <Widget>[
                GlossyCover(
                  seed: preset.name,
                  size: 34,
                  radius: 10,
                  icon: preset.url.startsWith('ws://localhost') || preset.url.contains('192.168')
                      ? Icons.router_rounded
                      : Icons.cloud_rounded,
                ),
                const SizedBox(width: GlossyDimens.related),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        preset.name,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: t.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${serverHostLabel(preset.url)} · ${preset.location}'
                        '${preset.operator.isEmpty ? '' : ' · ${preset.operator}'}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 11, color: t.textLow),
                      ),
                      if (preset.note != null) ...<Widget>[
                        const SizedBox(height: 3),
                        Text(
                          preset.note!,
                          style: TextStyle(fontSize: 10.5, height: 1.35, color: t.textMuted),
                        ),
                      ],
                    ],
                  ),
                ),
                if (matchesServer(preset.url, widget.client.serverUrl))
                  Icon(Icons.check_circle_rounded, size: 18, color: t.accent),
              ],
            ),
          ),
        ),
      const GlossyCaption('Notes'),
      GlossyCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            _Bullet(
              title: 'Addresses are normalised',
              body: 'A bare 192.168.1.24:8080 becomes ws://, a pasted https:// '
                  'becomes wss://, and a missing path becomes /ws.',
            ),
            _Bullet(
              title: 'Codes expire',
              body: 'A room code is six characters. The seat of someone who '
                  'drops keeps for a couple of minutes; an empty room is reaped.',
            ),
            _Bullet(
              title: 'Background limits stay',
              body: 'This prototype does not keep a socket alive in the '
                  'background — the Android client needs its foreground service '
                  'for that.',
              last: true,
            ),
          ],
        ),
      ),
    ];
  }
}

/// Loose comparison of two addresses, ignoring scheme/host casing.
bool matchesServer(String a, String b) =>
    normaliseServerUrl(a).toLowerCase() == normaliseServerUrl(b).toLowerCase();

class _DetailHeader extends StatelessWidget {
  const _DetailHeader({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 4),
      child: Row(
        children: <Widget>[
          GlossyIconButton(
            icon: Icons.arrow_back_rounded,
            tooltip: 'Settings',
            onTap: onBack,
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'INTEGRATIONS',
                  style: TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.4,
                    color: t.accent,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Listen Together',
                  style: TextStyle(
                    fontSize: 21,
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
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

/// The server address field, with the Cloudflare deployment prefilled and a
/// probe that opens a real socket.
class _ListenTogetherServerCard extends StatefulWidget {
  const _ListenTogetherServerCard({required this.client});

  final LtClient client;

  @override
  State<_ListenTogetherServerCard> createState() =>
      _ListenTogetherServerCardState();
}

class _ListenTogetherServerCardState extends State<_ListenTogetherServerCard> {
  /// The real address. The field shows it masked until it is being edited, so
  /// the Cloudflare subdomain the account label was minted from is not on
  /// screen by default — while what gets applied is always this value.
  late String _value = widget.client.serverUrl;
  late final TextEditingController _field =
      TextEditingController(text: serverAddressLabel(_value));
  final FocusNode _focus = FocusNode();
  bool _showFull = false;
  bool _testing = false;
  LtProbeResult? _result;
  bool _applied = false;

  bool get _editing => _focus.hasFocus || _showFull;

  String get _fieldText => _editing ? _value : serverAddressLabel(_value);

  @override
  void initState() {
    super.initState();
    _focus.addListener(_syncFieldText);
  }

  @override
  void dispose() {
    _focus.removeListener(_syncFieldText);
    _focus.dispose();
    _field.dispose();
    super.dispose();
  }

  /// Swaps the displayed text when editing starts or ends, without disturbing
  /// the caret while it is being typed into.
  void _syncFieldText() {
    if (!mounted) return;
    setState(() {
      if (_field.text != _fieldText) {
        _field.text = _fieldText;
        _field.selection = TextSelection(
          baseOffset: 0,
          extentOffset: _field.text.length,
        );
      }
    });
  }

  Future<void> _test() async {
    setState(() {
      _testing = true;
      _result = null;
    });
    final LtProbeResult result = await LtClient.probe(_value);
    if (!mounted) return;
    setState(() {
      _testing = false;
      _result = result;
    });
  }

  void _apply() {
    widget.client.configure(serverUrl: _value);
    setState(() {
      _applied = true;
      _value = widget.client.serverUrl;
      _showFull = false;
      _field.text = _fieldText;
    });
    Future<void>.delayed(const Duration(seconds: 2), () {
      if (mounted) setState(() => _applied = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final String normalised = normaliseServerUrl(_value);
    final bool changed = normalised != widget.client.serverUrl;

    return GlossyCard(
      accent: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Room server URL',
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: t.textPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Every phone that joins must use the same address. The Cloudflare '
            'deployment is filled in below; tap a preset to switch. The account '
            'label is masked on screen — tap the field to see or edit the real '
            'address.',
            style: TextStyle(fontSize: 11.5, height: 1.45, color: t.textLow),
          ),
          const SizedBox(height: GlossyDimens.related),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: t.chip,
              borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
              border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
            ),
            child: Row(
              children: <Widget>[
                Icon(Icons.dns_rounded, size: 16, color: t.accent),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _field,
                    focusNode: _focus,
                    onChanged: (String value) => setState(() => _value = value),
                    style: TextStyle(
                      fontSize: 12,
                      fontFamily: 'monospace',
                      color: t.textPrimary,
                    ),
                    decoration: InputDecoration(
                      border: InputBorder.none,
                      isDense: true,
                      hintText: 'wss://host/ws',
                      hintStyle: TextStyle(
                        fontSize: 12,
                        fontFamily: 'monospace',
                        color: t.textMuted,
                      ),
                    ),
                  ),
                ),
                GestureDetector(
                  onTap: () => setState(() {
                    _showFull = !_showFull;
                    _field.text = _fieldText;
                  }),
                  child: Icon(
                    _editing
                        ? Icons.visibility_off_rounded
                        : Icons.visibility_rounded,
                    size: 15,
                    color: t.textMuted,
                  ),
                ),
                const SizedBox(width: 8),
                GestureDetector(
                  onTap: () => setState(() {
                    _value = '';
                    _field.clear();
                  }),
                  child: Icon(Icons.close_rounded, size: 15, color: t.textMuted),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'resolves to  ${serverAddressLabel(normalised)}'
            '${_value.isEmpty ? '' : ' · ${serverSchemeLabel(_value)}'}',
            style: TextStyle(
              fontSize: 10.5,
              fontFamily: 'monospace',
              color: t.textMuted,
            ),
          ),
          const SizedBox(height: GlossyDimens.related),
          Row(
            children: <Widget>[
              Expanded(
                child: GlossyButton(
                  label: _applied ? 'Applied' : (changed ? 'Use this server' : 'Applied'),
                  icon: _applied || !changed ? Icons.check_rounded : Icons.save_rounded,
                  onPressed: changed ? _apply : null,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: GlossyButton(
                  label: 'Test connection',
                  tone: GlossyButtonTone.quiet,
                  icon: Icons.bolt_rounded,
                  busy: _testing,
                  onPressed: _testing ? null : _test,
                ),
              ),
            ],
          ),
          if (_result != null) ...<Widget>[
            const SizedBox(height: GlossyDimens.related),
            _ProbeLine(result: _result!),
          ],
        ],
      ),
    );
  }
}

class _ProbeLine extends StatelessWidget {
  const _ProbeLine({required this.result});

  final LtProbeResult result;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final Color color = result.reachable ? t.accent : Theme.of(context).colorScheme.error;
    final String text = result.reachable
        ? 'Answered in ${result.latencyMs ?? 0} ms'
            '${result.version == null ? '' : ' · ${result.version}'}'
        : (result.problem ?? 'No answer.');
    return Row(
      children: <Widget>[
        Icon(
          result.reachable ? Icons.check_circle_rounded : Icons.error_outline_rounded,
          size: 16,
          color: color,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: TextStyle(fontSize: 11.5, height: 1.4, color: t.textSecondary),
          ),
        ),
      ],
    );
  }
}

class _IdentityCard extends StatefulWidget {
  const _IdentityCard({required this.client});

  final LtClient client;

  @override
  State<_IdentityCard> createState() => _IdentityCardState();
}

class _IdentityCardState extends State<_IdentityCard> {
  late final TextEditingController _name =
      TextEditingController(text: widget.client.username);

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GlossyCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Display name',
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: t.textPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'The name the room lists you under. Both screens read the same '
            'value, so it is set once.',
            style: TextStyle(fontSize: 11.5, height: 1.45, color: t.textLow),
          ),
          const SizedBox(height: GlossyDimens.related),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: t.chip,
              borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
              border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
            ),
            child: Row(
              children: <Widget>[
                Icon(Icons.person_rounded, size: 16, color: t.accent),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _name,
                    maxLength: 24,
                    onChanged: (String value) =>
                        widget.client.configure(username: value),
                    style: TextStyle(fontSize: 13, color: t.textPrimary),
                    decoration: InputDecoration(
                      border: InputBorder.none,
                      isDense: true,
                      counterText: '',
                      hintText: 'Your name',
                      hintStyle: TextStyle(fontSize: 13, color: t.textMuted),
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

class _SettingRow extends StatelessWidget {
  const _SettingRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: <Widget>[
          Container(
            width: 34,
            height: 34,
            decoration: BoxDecoration(
              color: t.accentSoft,
              borderRadius: BorderRadius.circular(11),
            ),
            child: Icon(icon, size: 17, color: t.accent),
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 11, color: t.textLow),
                ),
              ],
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}

class _RowDivider extends StatelessWidget {
  const _RowDivider();

  @override
  Widget build(BuildContext context) => Divider(
        height: GlossyDimens.hairline,
        thickness: GlossyDimens.hairline,
        color: context.glossy.border,
      );
}

class _Chip extends StatelessWidget {
  const _Chip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: t.chip,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
        border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.w600, color: t.textMuted),
      ),
    );
  }
}

class _ThemeSwitch extends StatelessWidget {
  const _ThemeSwitch({required this.mode, required this.onChanged});

  final ThemeMode mode;
  final ValueChanged<ThemeMode> onChanged;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final bool dark = mode == ThemeMode.dark;
    return GestureDetector(
      onTap: () => onChanged(dark ? ThemeMode.light : ThemeMode.dark),
      child: Container(
        padding: const EdgeInsets.all(3),
        decoration: BoxDecoration(
          color: t.chip,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
          border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
        ),
        child: Row(
          children: <Widget>[
            Icon(
              dark ? Icons.dark_mode_rounded : Icons.light_mode_rounded,
              size: 14,
              color: t.accent,
            ),
            const SizedBox(width: 6),
            Text(
              dark ? 'Dark' : 'Light',
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: t.textPrimary),
            ),
          ],
        ),
      ),
    );
  }
}

class _Bullet extends StatelessWidget {
  const _Bullet({required this.title, required this.body, this.last = false});

  final String title;
  final String body;
  final bool last;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: EdgeInsets.only(bottom: last ? 0 : GlossyDimens.related),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 3,
            height: 14,
            margin: const EdgeInsets.only(top: 3),
            decoration: BoxDecoration(
              color: t.accent,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: t.textPrimary,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  body,
                  style: TextStyle(fontSize: 11, height: 1.4, color: t.textSecondary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
