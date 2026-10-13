#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /path/to/Glossy.apk [output-directory]" >&2
  exit 64
fi

APK="$(realpath "$1")"
if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 66
fi

if ! command -v rea >/dev/null 2>&1; then
  echo "REA CLI not found. Install it using the instructions in tools/reverse-engineering/README.md." >&2
  exit 127
fi

if [[ -z "${REA_JADX_MCP_JAR:-}" ]]; then
  echo "REA_JADX_MCP_JAR is not set. Configure the JADX engine as documented in tools/reverse-engineering/README.md." >&2
  exit 78
fi

APK_NAME="$(basename "$APK" .apk)"
OUT="${2:-build/apk-analysis/$APK_NAME}"
mkdir -p "$OUT"
OUT="$(realpath "$OUT")"

echo "Inspecting package metadata..."
rea inspect-android-package "$APK" > "$OUT/package.txt"

echo "Searching for Glossy classes..."
rea search-android-classes "$APK" "com.jay.glossy" > "$OUT/classes.txt"

echo "Inspecting MainActivity..."
rea inspect-android-class "$APK" "com.jay.glossy.MainActivity" > "$OUT/main-activity.txt"

{
  echo "APK=$APK"
  sha256sum "$APK"
  echo "Reports=$OUT"
  echo "Generated=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$OUT/target.txt"

echo "Analysis reports written to: $OUT"
