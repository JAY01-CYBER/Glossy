import 'package:flutter/material.dart';

import 'glossy_chrome.dart';
import 'glossy_tokens.dart';
import 'library_screen.dart';
import 'listen_together/lt_client.dart';
import 'listen_together/servers.dart';
import 'listen_together/together_screen.dart';
import 'settings_screen.dart';

void main() => runApp(const GlossyPreviewApp());

/// Host for the prototypes: a device frame so each screen is judged at phone
/// size, a light/dark switch to check both palettes, and the notes that explain
/// the decisions.
///
/// Two screens are designed here — the Library tab and Listen Together, plus
/// the Settings page that carries its room server address. This is a Flutter
/// implementation: Listen Together really opens a WebSocket and speaks the
/// protobuf room protocol, so the states it draws are the states the server
/// sent rather than mock data.
///
/// Deep links: `?screen=together|settings|library`, `?theme=light`,
/// `?name=YourName`, and `?filter=albums|songs|…` for the library.
class GlossyPreviewApp extends StatefulWidget {
  const GlossyPreviewApp({super.key});

  @override
  State<GlossyPreviewApp> createState() => _GlossyPreviewAppState();
}

class _GlossyPreviewAppState extends State<GlossyPreviewApp> {
  /// Dark unless the preview is opened with `?theme=light`.
  ThemeMode _mode = Uri.base.queryParameters['theme'] == 'light'
      ? ThemeMode.light
      : ThemeMode.dark;

  late final LtClient _client = LtClient(
    username: Uri.base.queryParameters['name'] ?? '',
  );

  late int _screen = _initialScreen();
  SettingsPage _settingsPage = SettingsPage.hub;

  static int _initialScreen() => switch (Uri.base.queryParameters['screen']) {
        'together' => 3,
        'settings' => 4,
        _ => 1,
      };

  @override
  void dispose() {
    _client.dispose();
    super.dispose();
  }

  void _go(int index, {SettingsPage? settingsPage}) {
    setState(() {
      _screen = index;
      if (settingsPage != null) _settingsPage = settingsPage;
    });
  }

  Widget _body(int index) {
    switch (index) {
      case 1:
        return LibraryScreen(onNavigate: _go);
      case 3:
        return TogetherScreen(
          client: _client,
          onNavigate: _go,
          onOpenServerSettings: () =>
              _go(4, settingsPage: SettingsPage.listenTogether),
        );
      case 4:
        return SettingsScreen(
          client: _client,
          mode: _mode,
          onModeChanged: (ThemeMode mode) => setState(() => _mode = mode),
          onNavigate: _go,
          initialPage: _settingsPage,
        );
      default:
        return _NotRedesigned(index: index, onGoToTogether: () => _go(3));
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Glossy · Flutter redesign',
      debugShowCheckedModeBanner: false,
      theme: glossyTheme(Brightness.light),
      darkTheme: glossyTheme(Brightness.dark),
      themeMode: _mode,
      home: _PreviewHost(
        mode: _mode,
        screen: _screen,
        onModeChanged: (ThemeMode mode) => setState(() => _mode = mode),
        onNavigate: _go,
        body: _body(_screen),
      ),
    );
  }
}

const double _phoneWidth = 390;
const double _phoneHeight = 844;

class _PreviewHost extends StatelessWidget {
  const _PreviewHost({
    required this.mode,
    required this.screen,
    required this.onModeChanged,
    required this.onNavigate,
    required this.body,
  });

  final ThemeMode mode;
  final int screen;
  final ValueChanged<ThemeMode> onModeChanged;
  final ValueChanged<int> onNavigate;
  final Widget body;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (BuildContext context, BoxConstraints constraints) {
            final Widget phone = Center(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: _DeviceFrame(child: body),
              ),
            );

            // Wide enough for the phone and the notes side by side; otherwise
            // the phone gets the whole canvas and the notes move out of the way.
            if (constraints.maxWidth >= 900) {
              return Padding(
                padding: const EdgeInsets.all(24),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    Expanded(child: phone),
                    const SizedBox(width: 28),
                    SizedBox(
                      width: 350,
                      child: SingleChildScrollView(
                        child: _DesignNotes(
                          mode: mode,
                          screen: screen,
                          onModeChanged: onModeChanged,
                          onNavigate: onNavigate,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }

            return Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
              child: Column(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          'Glossy · Flutter redesign',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: context.glossy.textPrimary,
                          ),
                        ),
                      ),
                      _ModeToggle(mode: mode, onModeChanged: onModeChanged),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Expanded(child: phone),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}

/// The phone: bezel, status bar, then the screen itself. Sized in logical
/// pixels and scaled down whole, so spacing stays true to a 390 dp device.
class _DeviceFrame extends StatelessWidget {
  const _DeviceFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Container(
      width: _phoneWidth,
      height: _phoneHeight,
      decoration: BoxDecoration(
        color: t.page,
        borderRadius: BorderRadius.circular(46),
        border: Border.all(color: t.border, width: 8),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.45),
            blurRadius: 44,
            spreadRadius: 2,
            offset: const Offset(0, 20),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(38),
        child: Material(
          color: t.page,
          child: Column(
            children: <Widget>[
              const _StatusBar(),
              Expanded(child: child),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBar extends StatelessWidget {
  const _StatusBar();

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(26, 14, 26, 2),
      child: Row(
        children: <Widget>[
          Text(
            '9:41',
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w600,
              color: t.textPrimary,
            ),
          ),
          const Spacer(),
          Icon(Icons.signal_cellular_alt_rounded,
              size: 15, color: t.textSecondary),
          const SizedBox(width: 6),
          Icon(Icons.wifi_rounded, size: 15, color: t.textSecondary),
          const SizedBox(width: 6),
          Icon(Icons.battery_full_rounded, size: 15, color: t.textSecondary),
        ],
      ),
    );
  }
}

/// Home and Mix are not redesigned here; the nav bar still has to answer.
class _NotRedesigned extends StatelessWidget {
  const _NotRedesigned({required this.index, required this.onGoToTogether});

  final int index;
  final VoidCallback onGoToTogether;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final String label = index == 0 ? 'Home' : 'Mix';
    return Padding(
      padding: const EdgeInsets.all(GlossyDimens.screenPadding),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Icon(Icons.construction_rounded, size: 30, color: t.textMuted),
          const SizedBox(height: GlossyDimens.related),
          Text(
            '$label is not part of this redesign',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: t.textPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Two screens are designed here: Library and Listen Together. '
            'Tap Together in the nav bar.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 11.5, height: 1.5, color: t.textLow),
          ),
          const SizedBox(height: GlossyDimens.section),
          GlossyButton(
            label: 'Open Listen Together',
            icon: Icons.people_alt_rounded,
            onPressed: onGoToTogether,
          ),
        ],
      ),
    );
  }
}

class _ModeToggle extends StatelessWidget {
  const _ModeToggle({required this.mode, required this.onModeChanged});

  final ThemeMode mode;
  final ValueChanged<ThemeMode> onModeChanged;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: t.chip,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
        border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
      ),
      child: Row(
        children: <Widget>[
          _segment(t, 'Light', Icons.light_mode_rounded, ThemeMode.light),
          _segment(t, 'Dark', Icons.dark_mode_rounded, ThemeMode.dark),
        ],
      ),
    );
  }

  Widget _segment(
    GlossyTokens t,
    String label,
    IconData icon,
    ThemeMode value,
  ) {
    final bool active = mode == value;
    return GestureDetector(
      onTap: () => onModeChanged(value),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: active ? t.accent : null,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
        ),
        child: Row(
          children: <Widget>[
            Icon(icon, size: 14, color: active ? t.onAccent : t.textSecondary),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w600,
                color: active ? t.onAccent : t.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DesignNotes extends StatelessWidget {
  const _DesignNotes({
    required this.mode,
    required this.screen,
    required this.onModeChanged,
    required this.onNavigate,
  });

  final ThemeMode mode;
  final int screen;
  final ValueChanged<ThemeMode> onModeChanged;
  final ValueChanged<int> onNavigate;

  static const List<(String, String)> _togetherNotes = <(String, String)>[
    (
      'The mode is chosen, not inferred',
      'The app drew the room-code field first and let its contents act as the mode switch: eight characters hid "Create room" and revealed "Join room", so joining had no visible entry point and creating a room asked for a code nobody had. Two segments now own that choice, and the code field only exists for Join.',
    ),
    (
      'Codes are six characters',
      'The server checks len == 6 while the old button stayed disabled until eight were typed. Six cells, upper-cased, with a live counter.',
    ),
    (
      'Waiting is drawn',
      'isCreatingRoom was written on tap and never read, so the screen looked idle while the request was in flight. The pending card is driven by the client phase, and Cancel is always available.',
    ),
    (
      'Every refusal has a sentence',
      'Room not found, room full, at capacity, not the host, session expired — each maps to one line plus the fix, and server problems name Settings → Integrations → Listen Together.',
    ),
    (
      'The host approves at the door',
      'A join request is a card with Let in / Decline, and the guest sees "waiting for the host" until it is answered.',
    ),
    (
      'This is a real client',
      'It opens wss:// to the Cloudflare Worker and speaks the protobuf room protocol. The WEBSOCKET panel at the bottom of each state is that traffic, not a log of fake data.',
    ),
  ];

  static const List<(String, String)> _settingsNotes = <(String, String)>[
    (
      'The Cloudflare server had no entry',
      'The app seeds Metrolist and The Meowery and refreshes from Echo Music\'s server.json, so the Worker in metroserver-worker/ was reachable only by pasting a URL. It is the first preset here, and the address is a first-class field.',
    ),
    (
      'Test connection opens a socket',
      'The health route carries no CORS headers, so a browser cannot fetch it. The probe performs the capability handshake instead and reports the build and the round trip.',
    ),
    (
      'Addresses are normalised',
      '192.168.1.24:8080 → ws://, a pasted https:// → wss://, a missing path → /ws, and the resolved address is shown under the field before you apply it.',
    ),
    (
      'One identity, set once',
      'The display name is shared with the Listen Together screen, so a room lists the same name you set here.',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final List<(String, String)> notes =
        screen == 4 ? _settingsNotes : _togetherNotes;
    final String title = screen == 4 ? 'Settings' : 'Listen Together';
    final String blurb = screen == 4
        ? 'Flutter widget tree · where the room server lives'
        : 'Flutter widget tree · live WebSocket client';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          title,
          style: TextStyle(
            fontSize: 21,
            fontWeight: FontWeight.w600,
            color: t.textPrimary,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          blurb,
          style: TextStyle(fontSize: 11.5, color: t.textLow),
        ),
        const SizedBox(height: 16),
        Row(
          children: <Widget>[
            _screenChip(t, 'Library', 1),
            const SizedBox(width: 6),
            _screenChip(t, 'Together', 3),
            const SizedBox(width: 6),
            _screenChip(t, 'Settings', 4),
          ],
        ),
        const SizedBox(height: 12),
        _ModeToggle(mode: mode, onModeChanged: onModeChanged),
        const SizedBox(height: 22),
        for (final (String noteTitle, String body) in notes) ...<Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Container(
                width: 3,
                height: 15,
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
                      noteTitle,
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: t.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      body,
                      style: TextStyle(
                        fontSize: 11.5,
                        height: 1.45,
                        color: t.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
        ],
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            color: t.chip,
            borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
            border: Border.all(
              color: t.chipOutline,
              width: GlossyDimens.hairline,
            ),
          ),
          child: Text(
            'Listen Together talks to ${serverAddressLabel(kGlossyCloudflareUrl)} '
            'by default — the account label is masked in the UI and only the '
            'address field reveals the real one. Create a room in one window and '
            'join with the code in another to see both sides.',
            style: TextStyle(fontSize: 11, height: 1.5, color: t.textSecondary),
          ),
        ),
        const SizedBox(height: 14),
        Text(
          'The engine stays Kotlin: this redesign draws the UI and speaks the '
          'existing wire protocol. Wiring it into the app would be a '
          'MethodChannel layer over the current ListenTogetherManager.',
          style: TextStyle(fontSize: 11, height: 1.5, color: t.textMuted),
        ),
      ],
    );
  }

  Widget _screenChip(GlossyTokens t, String label, int index) {
    final bool active = screen == index;
    return GestureDetector(
      onTap: () => onNavigate(index),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: active ? t.accentSoft : t.chip,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
          border: Border.all(
            color: active ? t.accentSoft : t.chipOutline,
            width: GlossyDimens.hairline,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w600,
            color: active ? t.accent : t.textSecondary,
          ),
        ),
      ),
    );
  }
}
