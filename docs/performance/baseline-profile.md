# Baseline Profiles for Glossy

## Design

The `:baselineprofile` test module generates profiles by executing real UI journeys against
the `:app` release-equivalent, non-minified target. The app consumes the generated rules via
the AndroidX Baseline Profile Gradle plugin. The generator includes startup in the Startup
Profile and exercises the stable primary journeys that can run without an account or remote
catalogue data.

Covered journeys:
- cold launch and first-run guest onboarding when it is shown;
- Home rendering and vertical scrolling;
- Mix, Search, and Library navigation;
- search-field interaction when the accessibility node is available;
- History, Stats, Liked, and Downloaded destinations when visible;
- Settings sections when exposed by the current layout.

Remote song playback, populated playlists, lyrics, integrations, and other data-dependent
destinations are not claimed as covered by this deterministic baseline test. Add fixture-backed
journeys for those flows rather than relying on network data or tapping arbitrary screen
coordinates.

## Generate locally

Use a physical device or emulator running Android 13 (API 33) or newer, with the app's
configured Android SDK and Java 21 available:

```bash
./gradlew :app:generateFossReleaseBaselineProfile
./gradlew :app:generateGmsReleaseBaselineProfile
./gradlew :app:generateIzzyReleaseBaselineProfile
```

The generated, variant-specific files are saved under
`app/src/<variant>/generated/baselineProfiles/` because `saveInSrc = true` is enabled.
Review the generated diff and commit the real generated rules. The scheduled GitHub Actions
workflow uploads each variant's generated files as an artifact; it does not silently commit them.
Download the artifacts or generate locally, review the output, then commit the profile files. Do
not hand-write broad package-wide wildcard rules: they are not a substitute for profiles collected
from actual execution.

To build a release variant after generation:

```bash
./gradlew :app:assembleFossRelease
./gradlew :app:assembleGmsRelease
./gradlew :app:assembleIzzyRelease
```

## Verify packaging

Open the resulting APK in Android Studio's APK Analyzer and confirm
`assets/dexopt/baseline.prof` exists. The profile is compiled/rewritten during the Android
build, so do not expect the source `.txt` file to appear unchanged inside the APK.

Measure performance on a physical device using Macrobenchmark before claiming a speedup.
Profile generation establishes code-path coverage; it does not, by itself, prove a specific
startup or scrolling improvement.
