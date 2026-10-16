# Glossy APK analysis (developer-only)

This folder documents a **local analysis workflow for APKs built from this repository**.
It is not an Android runtime dependency and is never packaged into Glossy.

## What this is for

- Inspect the built APK manifest, SDK declarations, package metadata, classes, and resources.
- Trace the app's own `MainActivity` and related classes when investigating a crash or a
  release-only behavior.
- Keep analysis output outside the source tree so APK dumps and decompiled sources are not
  accidentally committed.

## Prerequisites

REA is an external developer tool. Install it separately using its upstream instructions:
<https://github.com/morluto/rea>. For Android APK analysis, REA documents a full JDK 17+
and a compatible `jadx-headless-mcp` JAR. Set `REA_JADX_MCP_JAR` to the absolute JAR path
before invoking the Android analysis commands. The tool does not install the JDK, Android SDK,
JADX engine, or emulator for you.

## Run against your own Glossy APK

From the repository root:

```bash
npm install --global rea-agents
export REA_JADX_MCP_JAR="/absolute/path/to/jadx-headless-mcp-all.jar"
tools/reverse-engineering/analyze-apk.sh app/build/outputs/apk/foss/release/app-universal-release.apk
```

The exact APK filename can differ by workflow; pass the actual APK path as the first argument.
On Windows, run the equivalent REA commands from PowerShell as documented upstream.

The script writes reports to `build/apk-analysis/<apk-name>/`. Reports can include decompiled
implementation details, so keep them local and do not upload them unless they contain no
credentials, personal data, or other private material.

## Limits and safety

Static analysis is evidence about the APK bytes that were inspected; it does not prove runtime
behavior, server behavior, or APK authenticity. Analyze only apps/APKs you own or have
permission to inspect. This tooling is intentionally kept separate from production code.
