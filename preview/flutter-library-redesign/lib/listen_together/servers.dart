/// Known room servers, and the address handling the settings screen shares with
/// the client.
///
/// The app's registry (`ListenTogetherServers.kt`) seeds Metrolist and The
/// Meowery and refreshes from Echo Music's published `server.json`. This list is
/// the same shape with Glossy's own Cloudflare deployment first, because that is
/// the one the redesign is verified against.
library;

class LtServerPreset {
  const LtServerPreset({
    required this.name,
    required this.url,
    required this.location,
    required this.operator,
    this.note,
  });

  final String name;
  final String url;
  final String location;
  final String operator;
  final String? note;
}

/// The Worker + Durable Object deployment committed in `metroserver-worker/`.
const String kGlossyCloudflareUrl =
    'wss://glossy-listen-together.izybro110.workers.dev/ws';

/// `python metroserver/metro_server.py --print-urls` prints this shape for a
/// phone on the same Wi-Fi.
const String kLocalDevUrl = 'ws://192.168.1.24:8080/ws';

const List<LtServerPreset> kLtServerPresets = <LtServerPreset>[
  LtServerPreset(
    name: 'Glossy on Cloudflare',
    url: kGlossyCloudflareUrl,
    location: 'Cloudflare edge',
    operator: 'Glossy',
    note: 'Worker + one Durable Object. Free plan, hibernating sockets.',
  ),
  LtServerPreset(
    name: 'Metrolist',
    url: 'wss://metroserverx.meowery.eu/ws',
    location: 'Poland',
    operator: 'Metrolist',
  ),
  LtServerPreset(
    name: 'The Meowery',
    url: 'wss://rx.meowery.eu/ws',
    location: 'Poland',
    operator: 'Nyx',
  ),
  LtServerPreset(
    name: 'This computer (Wi-Fi)',
    url: kLocalDevUrl,
    location: 'Your LAN',
    operator: 'You',
    note: 'Run the bundled stdlib server on the same network.',
  ),
];

/// Accepts what a person actually types. A bare `192.168.1.24:8080` used to
/// reach OkHttp and throw out of the tap handler; here it becomes `ws://…`, a
/// pasted `https://…` becomes `wss://…`, and a missing path becomes `/ws`.
String normaliseServerUrl(String raw) {
  String value = raw.trim();
  if (value.isEmpty) return value;

  if (value.startsWith('https://')) {
    value = 'wss://${value.substring('https://'.length)}';
  } else if (value.startsWith('http://')) {
    value = 'ws://${value.substring('http://'.length)}';
  } else if (!value.contains('://')) {
    value = 'ws://$value';
  }

  while (value.endsWith('/')) {
    value = value.substring(0, value.length - 1);
  }

  final Uri? uri = Uri.tryParse(value);
  if (uri == null || uri.host.isEmpty) return raw.trim();
  if (uri.path.isEmpty || uri.path == '/') return '$value/ws';
  return value;
}

/// A `*.workers.dev` host carries the account label the Cloudflare subdomain
/// was minted from, which is the user's own handle and does not belong in a
/// screenshot. Every display in the UI goes through this, so the account label
/// never leaves the address field: `glossy-listen-together.account.workers.dev`.
String maskAccountLabel(String host) {
  final List<String> labels = host.split('.');
  const String suffix = 'workers.dev';
  if (labels.length < 3 || labels.sublist(labels.length - 2).join('.') != suffix) {
    return host;
  }
  final List<String> masked = List<String>.of(labels);
  masked[masked.length - 3] = 'account';
  return masked.join('.');
}

/// The host as the UI may show it — `glossy-listen-together.account.workers.dev`
/// for a Cloudflare deployment, the plain host everywhere else.
String serverHostLabel(String url) {
  final Uri? uri = Uri.tryParse(normaliseServerUrl(url));
  if (uri == null || uri.host.isEmpty) return url;
  return maskAccountLabel(uri.host);
}

/// The whole address with the account label masked, for lines that show the URL.
String serverAddressLabel(String url) {
  final String normalised = normaliseServerUrl(url);
  final Uri? uri = Uri.tryParse(normalised);
  if (uri == null || uri.host.isEmpty) return url;
  return normalised.replaceFirst(uri.host, maskAccountLabel(uri.host));
}

/// What the status row and the settings list call a server: its preset name when
/// it is one of the known ones, otherwise the masked host.
String serverDisplayName(String url) {
  final String normalised = normaliseServerUrl(url).toLowerCase();
  for (final LtServerPreset preset in kLtServerPresets) {
    if (normaliseServerUrl(preset.url).toLowerCase() == normalised) {
      return preset.name;
    }
  }
  return serverHostLabel(url);
}

/// `ws` / `wss`, so the UI can say whether the room traffic is encrypted.
String serverSchemeLabel(String url) {
  final Uri? uri = Uri.tryParse(normaliseServerUrl(url));
  return uri?.scheme ?? 'ws';
}

