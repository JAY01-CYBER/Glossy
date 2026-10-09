# Glossy Baseline Profiles

This module generates Android Baseline Profiles for Glossy's main user journeys.

Covered journeys:
- Cold startup and initial home rendering (included in the Startup Profile).
- Home-screen scrolling.
- Search and Library navigation when those controls are exposed by the current UI.

The generator uses a Gradle-managed Pixel 2 / Android 15 (API 35) AOSP device, so it can run in GitHub Actions without a manually connected phone.

## Generate locally

```bash
./gradlew :app:generateGmsReleaseBaselineProfile
./gradlew :app:generateFossReleaseBaselineProfile
```

The GitHub Actions workflow `.github/workflows/baseline-profile.yml` runs weekly or on manual dispatch. It generates both profiles, copies them into the flavor source sets, assembles both release APKs to validate profile packaging, uploads the outputs, and opens/updates a pull request with the generated profile files.

Baseline Profiles improve compilation/optimization of frequently used code paths; they do not guarantee a fixed percentage speedup. Measure startup and jank on representative physical devices to quantify the impact.
