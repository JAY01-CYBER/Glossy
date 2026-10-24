# Glossy — everything since the 0.0.4 release: 24 commits, 69 files, +6976 / −773

Contribution branch: **`fix/autoplay-stop-and-spotify-lyrics`** on the fork `izybro110-gif/Glossy`
Head: **`d139c41c`** · Base: **`JAY01-CYBER/Glossy:main` @ `99f49d8e`** (the tree that ships as `versionCode 4 / versionName "0.0.4"`)

This branch is the whole delta from that released tree to now. Every commit, every changed file and every bug that was found and cleared is registered below, with the cause of each one so it can be reviewed on its merits rather than trusted.

One measurement detail: `versionName "0.0.4"` was set upstream on 2026-09-25 (`46c85669`) and is unchanged since, so if the APK you published as 0.0.4 was built from a commit *older* than `main`'s current tip, an installed 0.0.4 APK is missing a little more than this list. Everything here is measured against `main` as it stands, which is what a merge of this PR would arrive on.

---

## 0. At a glance

| | |
|---|---|
| Commits | 24 (22 non-merge + 2 merges of `JAY01-CYBER:main`) |
| Files touched | 69 — 46 modified, 2 deleted, 21 added (6 new Kotlin files, 12 preview pages, 2 build files, 1 submodule gitlink) |
| Lines | +6976 / −773. Of those, +4166 are the 12 self-contained preview pages (pure HTML, nothing shipped in the APK) |
| App code | 52 files under `app/`, +2677 / −773; +133 across `settings.gradle.kts`, `.gitmodules` and `seaglass-build/` |
| Bugs fixed | 17 (B1–B17 below) |
| New work | 12 features (F1–F12 below) |
| Version | unchanged on purpose — `versionCode 4 / versionName "0.0.4"`, so this lands as the 0.0.4 line until you bump it |

---

## 1. Bugs found and cleared

Every entry: what a listener saw → why it happened → what now happens → the files that carry it.

### B1 — Autoplay off did not stop playback
- **Symptom.** With *Autoplay* turned off, the next song still started when the current one ended. The switch's own description promises "automatically play the next song when the current one ends".
- **Cause.** The autoplay preference was only consulted when the player ran off the end of the *whole queue*. A loaded playlist never reaches the end of the queue: ExoPlayer rolls into the next media item by itself, so the check was unreachable in normal use.
- **Fix.** The automatic track transition is now intercepted: with autoplay off the player pauses at the end of the song instead of starting the next one, while the queue, the notification and the position in it stay exactly where they were.
- **Files.** `playback/MusicService.kt`
- Related in the same commit: a song that had been written to the lyrics table as "nothing found" now gets **one real retry per run**, so a provider that was merely slow once does not poison the song forever. `playback/MusicService.kt`

### B2 — Spotify lyrics never arrived on a signed-in account
- **Symptom.** Signed in to Spotify, a working cookie stored, and the player still reported no lyrics for songs Spotify's own endpoint had them for.
- **Cause.** Spotify's lyrics need a bearer token, and minting it means loading the web player in an offscreen WebView and reading the token it hands itself — seconds. That mint ran on the caller's job, inside a lyrics race that gives each provider 3.5 s, so it was **cancelled before it could ever finish**, nothing was cached, and the next lookup paid the same wait again. A whole session could hold a valid cookie and never get a single Spotify answer out of it.
- **Fix.** `SpotifySession.warmUp()` starts the mint on its own scope so it survives any caller's timeout and the token is waiting in the cache for whoever asks next; the provider kicks a warm-up off before it asks for lyrics; and Spotify gets its own 6 s lookup budget (still inside the race's 12 s ceiling, and still leaving room for the fallback tiers) for the cold case.
- **Files.** `spotify/SpotifySession.kt`, `lyrics/SpotifyLyricsProvider.kt`, `lyrics/LyricsHelper.kt`

### B3 — Spotify lyrics parsed as "not found" although the response had them
- **Symptom.** The endpoint answered, the payload contained timed lyrics, and the app reported no usable lyrics.
- **Cause.** The provider read a `syncedLines` array whose entries were objects. The endpoint actually answers with `lyrics.lines`, `startTimeMs` **as a string**, and `words` as finished text — so every line failed to deserialise and was dropped.
- **Fix.** The real shape is read (and the old one is still accepted); lines with no timestamp are dropped so the lyric view animates against real times; and Spotify now leads the default provider order, because it is the one provider whose lyrics come from the listener's own account, with real timestamps, and the only one the app is already signed in to.
- **Files.** `lyrics/SpotifyLyricsProvider.kt`, `lyrics/LyricsProviderRegistry.kt`

### B4 — The provider was enabled only by a buried switch
- **Symptom.** Sign-in alone did not turn Spotify lyrics on; you had to find a toggle inside Content settings.
- **Cause.** `isEnabled` was `?: false`, so the provider was off for everybody who had never answered that switch.
- **Fix.** Enabled when the switch says so — and, until it has been answered for itself, enabled when a Spotify account is signed in. A stored `false` still means false, so switching it off works.
- **Files.** `lyrics/SpotifyLyricsProvider.kt`

### B5 — The settings switch contradicted the provider
- **Symptom.** Content settings showed *Spotify lyrics: off* while Spotify lyrics were in fact being fetched, so a song with none gave no reason to look any further.
- **Cause.** The switch's default was a flat `false`, matching nothing about the provider's real state.
- **Fix.** The default mirrors the sign-in state, which is the state the provider is enabled by.
- **Files.** `ui/screens/settings/ContentSettings.kt`

### B6 — Every mini player style folded into a squat rounded rectangle
- **Symptom.** Studio, Glossy Special Edition, Legacy and Modern all folded into a wrong-looking shape on scroll.
- **Cause.** The redesign folded *every* style into a small round remnant. The bar keeps its full height while the remnant's width is a fraction of the screen, so clipping it with a 50 % corner drew a squat rounded rectangle, not the circle it was aiming for.
- **Fix.** Only the Apple Music bar folds, which is what its own design describes; the other styles keep the shape they draw.
- **Files.** `ui/player/MiniPlayer.kt`

### B7 — The fold crashed on the corner percent
- **Symptom.** Crash on the first fold animation.
- **Cause.** The fold springs from 50 to 0 with a low-bouncy spec (`DampingRatioLowBouncy = 0.75`), so the first bounce overshoots the target and dips to about **−1.4**. `RoundedCornerShape` rejects a percent outside `[0, 100]` by throwing, so every fold recomposition crashed on `PercentCornerSize`.
- **Fix.** The percent is clamped. Visually lossless: there is no rounder corner than a full-round one.
- **Files.** `ui/player/MiniPlayer.kt`

### B8 — Debounced scroll collapse fired at half its threshold
- **Symptom.** The chrome collapsed after a nudge rather than a swipe (12 px instead of the intended 24 px).
- **Cause.** A nested scroll reports both what was **offered** before the child handled it and what the child actually **consumed**, and the state tracked both — every gesture was counted twice.
- **Fix.** Only the consumed delta is measured, after the fact, in `onPostScroll`.
- **Files.** `ui/component/AppScrollState.kt`

### B9 — The lyrics clipped the first letter of every line
- **Symptom.** Every lyric line lost the left edge of its first glyph.
- **Cause.** The line put its horizontal inset **before** the blur and the `graphicsLayer` instead of after. Both draw through a layer and clip to its bounds, so the padding shrank those bounds down to the text box and the item's rounded clip then took the left edge off the first glyph.
- **Fix.** The inset is applied after the layer, so the bounds are the item's own.
- **Files.** `ui/component/LyricsLine.kt`, `ui/component/OriginalLyrics.kt`, `ui/screens/HomeScreen.kt` (dropped the now-unused sea-glass import in the same housekeeping commit)

### B10 — Quick picks showed the neighbours of a seed, not your listening
- **Symptom.** Quick picks answered with one artist's neighbours rather than the music actually played — a stretch of one artist showed that artist's orbit.
- **Cause.** The row was built only from the **related** songs of a recent or most-played seed; the query seeds itself from your history but answers with the orbit around it.
- **Fix.** The last eight tracks from the listen history lead the row, with the related-songs fill kept behind them.
- **Files.** `viewmodels/HomeViewModel.kt`

### B11 — Crash reports were filed as Metrolist
- **Symptom.** A Glossy crash report was titled "Metrolist crash report" and written as `metrolist_crash_*.txt`, so it read as someone else's app.
- **Cause.** Fork leftovers in the reporting path.
- **Fix.** Reported as Glossy.
- **Files.** `ui/screens/CrashActivity.kt`, `res/values/metrolist_strings.xml`

### B12 — Sea glass crashed the app below Android 13
- **Symptom.** On Android 10 the login screen died with `NoClassDefFoundError` the moment the glass pass ran.
- **Cause.** `Modifier.seaGlass` resolves `android.graphics.RuntimeShader`, which only exists from Android 13 (API 33) — the class load failed before any of SeaGlass's own tier checks could fall back to a software path.
- **Fix.** Availability is gated **in Glossy**, before SeaGlass is reached, with a painted fallback below API 33 instead of a flat fill, so older devices get a designed surface rather than a crash or a hole. `SeaGlassSupport.kt` owns the single answer to "is the glass available here".
- **Files.** `ui/component/SeaGlassSupport.kt`, `ui/screens/GlossyLoginScreen.kt`, `preview/glossy-seaglass-android10.html`, `preview/glossy-seaglass-app.html`

### B13 — Queue rows were unreadable over artwork
- **Symptom.** The queue's rows, its title and its header lines sat at a contrast you could barely read against a saturated cover.
- **Cause.** They drew with the colour scheme's own text colours, which are correct on a themed surface and wrong on blurred album art.
- **Fix.** A new `LocalListRowContentColor` composition local lets a list state its own row colour; ordinary lists keep the theme's colours exactly as they were, and the player's queue — the drawer over blurred artwork — provides white, the same answer the transport controls beside it already gave.
- **Files.** `ui/component/Items.kt`, `ui/player/Queue.kt`, `ui/player/applemusic/AppleMusicQueueView.kt`

### B14 — The vendored Compose module failed `checkAarMetadata`
- **Symptom.** The Android build failed with eleven "AAR metadata" errors.
- **Cause.** `dev.seaglass.compose` pinned `compileSdk 36` while its transitive Compose dependencies (material3 1.5.0-alpha25, compose 1.12.0-beta01) now require 37.
- **Fix.** It compiles against the app's own `compileSdk`.
- **Files.** `seaglass-build/seaglass-compose.gradle.kts`

### B15 — Gradients and shaders rebuilt on every frame (scroll jank)
- **Symptom.** Jank on every scrolling lyrics/artist list, on the player backdrop while lyrics move, and on the artwork fade.
- **Cause.** Several surfaces built their `Brush` — and the Skia shader behind it — inside the draw pass, so a fresh pair was allocated on every frame they drew.
- **Fix.** They are hoisted, or built in `drawWithCache`, the one hook that hands back the size and only re-runs when that size changes.
- **Files.** `ui/utils/FadingEdge.kt`, `ui/component/GlassBackdrop.kt`, `ui/component/SeaGlassPill.kt`, `ui/component/SeaGlassSupport.kt`, `ui/player/applemusic/NowPlayingContentAppleMusic.kt`

### B16 — Audio boost was dead weight that looked live
- **Symptom.** An Audio boost control that promised something the pipeline could not deliver.
- **Cause.** The boost path was a no-op as wired up, sitting in the menu and in settings as if it worked.
- **Fix.** Removed outright — the sound-fx entry, the dialog, the repository plumbing and the menu/settings rows are gone rather than left as a switch that does nothing. (`eq`/sound-fx volume handling itself is untouched.)
- **Files.** `eq/soundfx/AudioBoost.kt` (deleted), `eq/soundfx/SoundFx.kt`, `eq/soundfx/SoundFxRepository.kt`, `ui/component/AudioBoostDialog.kt` (deleted), `ui/menu/PlayerMenu.kt`, `ui/screens/settings/PlayerSettings.kt`, `ui/screens/settings/AppearanceSettings.kt`

### B17 — An installed APK could not be told apart from the one before it
- **Symptom.** "Did the fix reach the phone?" was unanswerable — three separate times during this work, once per round of testing.
- **Cause.** `versionCode`/`versionName` are identical for every build of 0.0.4, so nothing inside the app distinguished an APK carrying a fix from one that predated it.
- **Fix.** `BuildConfig.BUILD_STAMP` = build timestamp **plus** the commit it was built from (`GITHUB_SHA`'s first seven characters in CI, `local` otherwise), shown at the foot of Settings. A new stamp on every build is the point, so the `BuildConfig` recompilation is accepted deliberately.
- **Files.** `app/build.gradle.kts`

---

## 2. New work on the branch

### F1 — Sea glass, vendored and built
SeaGlass publishes no artifacts, so it is vendored as a pinned submodule (`github.com/mweinbach/SeaGlass`) and built as two ordinary subprojects — `:seaglass-core`, `:seaglass-compose` — not as an `includeBuild`, because AGP refuses two AGP versions in one Gradle invocation and SeaGlass pins its own.
- The submodule stays a **pristine checkout**: its sources, manifest and source sets come from upstream untouched, while the build files that run are Glossy's own under `seaglass-build/` (upstream's pin a Java 17 toolchain and there is no toolchain download repository here — F-Droid cannot use the foojay resolver — so nothing could provision one). They are named after their modules rather than `core`/`compose` because this repository ignores `core.*`, the core-dump pattern, which would quietly leave the core module's build file out of every clone.
- A clone that skipped submodules fails fast with the command that fixes it, because the app imports `dev.seaglass.*`: the submodule is required, not optional.
- **Files.** `settings.gradle.kts`, `.gitmodules`, `third_party/SeaGlass` (gitlink), `seaglass-build/seaglass-core.gradle.kts`, `seaglass-build/seaglass-compose.gradle.kts`
- **Build note for reviewers:** `git submodule update --init --recursive` before the first Gradle run.

### F2 — One sea-glass material instead of five tunings
The glass had been tuned five separate times (0.44/0.70 on the sign-in pills, 0.38/0.66 on the guest pill, 0.40/0.72 on the section panels, 0.62/0.45 on onboarding, 0.44/0.70 on the hero play pill), so the same material read at a different strength on every screen and every new surface meant a sixth tuning. It is cut from **one** place now — `SeaGlassSupport` (availability + `Modifier.seaGlass`), `SeaGlassPanel` (the section pane) and `SeaGlassPill` (the pill, lifted out of the login card and put on the hero play button too) — and every screen adopts it: Home, Library, Mix, onboarding, login, the app chrome.
- **Files.** `ui/component/SeaGlassSupport.kt`, `ui/component/SeaGlassPanel.kt`, `ui/component/SeaGlassPill.kt`, `ui/component/GlossyDesignSystem.kt`, `ui/component/AppNavigation.kt`, `ui/component/MediaDetailHero.kt`, `ui/screens/GlossyLoginScreen.kt`, `ui/screens/WelcomeScreen.kt`, `ui/screens/HomeScreen.kt`, `ui/screens/library/LibraryScreen.kt`, `ui/screens/library/LibraryMixScreen.kt`

### F3 — Materialistic (tonal) mini player background style
A tonal background style beside the existing ones, so the bar can follow the theme instead of the artwork.
- **Files.** `ui/player/MiniPlayer.kt`, `ui/screens/settings/AppearanceSettings.kt`, `constants/PreferenceKeys.kt`

### F4 — Apple Music mini player style that folds away on scroll
Square artwork, the playhead as a squiggle under the title, transport in a filled capsule; scrolling a list folds the bar into the round artwork bubble and the top of the page springs it back. It is a style of its own, not a change to the Special Edition pill.
- **Files.** `ui/player/MiniPlayer.kt`, `MainActivity.kt`, `ui/component/AppScrollState.kt`, `constants/PreferenceKeys.kt`

### F5 — Page-open transition for anything that is not a tab
Settings, artists, playlists and albums now rise into place (fade, a third of a slide, a slight scale) while the page being left recedes instead of sliding away, so opening a detail page no longer looks like a tab switch.
- **Files.** `ui/player/PlayerOpenBloom.kt`, `MainActivity.kt`, `ui/component/BottomSheet.kt`, `res/values/glossy_strings.xml`

### F6 — Audio-format badge
The stream actually playing (codec/tier) is shown where the listener is, instead of only in a settings dialog.
- **Files.** `ui/component/AudioFormatBadge.kt`, `ui/player/Player.kt`, `res/values/glossy_strings.xml`

### F7 — Unsung lyric lines recede with a depth blur
Lines away from the one being sung recede by **distance as well as alpha**, keyed off the same active line the fade already uses. The blur is a **step**, not a tween, on purpose: a radius that moves every frame re-allocates the `RenderEffect` pass on each frame. `Modifier.blur` is a no-op below Android 12, so those devices simply keep the fade they always had.
- **Files.** `ui/component/LyricsCommon.kt`, `ui/component/LyricsLine.kt`, `ui/component/OriginalLyrics.kt`, `ui/component/ExperimentalLyrics.kt`

### F8 — Import your own font, and a larger default lyric size
A listener can import a font file for the app and for the lyrics, and the lyric default is 36 sp.
- **Files.** `ui/screens/settings/AppFontSettingsScreen.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`, `ui/screens/settings/PlayerSettings.kt`, `ui/screens/settings/AppearanceSettings.kt`, `res/values/glossy_strings.xml`

### F9 — Redesigned queue header
The queue's own header (title, count, controls) replaces the borrowed one.
- **Files.** `ui/player/Queue.kt`, `ui/player/applemusic/AppleMusicQueueView.kt`, `res/values/glossy_strings.xml`

### F10 — Consecutive duplicate lyric lines collapsed
Providers do stamp the same sentence two or three times in a row, and a listener reads that stack as the app being broken. The repeat is dropped rather than merged, because it carries its own timestamps; the background-vocal flag has to match too, since an echo is a different line on screen.
- **Files.** `lyrics/LyricsUtils.kt`

### F11 — Opus tier capped at 130 kbps
YouTube's Opus tiers step through roughly 50/70/130/160 kbps. The ceiling lets the tier land on the 130 kbps step, keeping Opus's efficiency while cutting most of the top tier's download, whose extra bits buy little a phone can resolve.
- **Files.** `utils/YTPlayerUtils.kt`

### F12 — Twelve preview pages, one per change
Self-contained HTML renderings of each change as before/after, with the pixel scans that located them (the lyric clip, the pill geometry, the fold, the below-API-33 fallback). They are documentation only — nothing here ships in the APK — and they are what was used to check the work without a device in hand.
- **Files.** `preview/` (12 new pages, listed in §3)

---

## 3. Every file on the branch, registered

Baseline for every row is `99f49d8e`. Short names are relative to `app/src/main/kotlin/com/jay/glossy/` unless the row says otherwise; §3 ends with all 69 paths verbatim.

### Build / project

| File | ± | What it carries |
|---|---|---|
| `settings.gradle.kts` | +34 | F1 — `:seaglass-core` / `:seaglass-compose` subprojects whose `projectDir` is the submodule and whose build file is Glossy's, plus the fail-fast check with the `git submodule update --init --recursive` hint |
| `.gitmodules` | +3 | F1 — the `third_party/SeaGlass` submodule entry |
| `third_party/SeaGlass` | gitlink | F1 — pinned vendored checkout (upstream `mweinbach/SeaGlass`) |
| `seaglass-build/seaglass-core.gradle.kts` | +42 | F1 — Glossy's build file for the vendored core module (repo toolchain in place of upstream's Java 17 pin) |
| `seaglass-build/seaglass-compose.gradle.kts` | +53 | F1 + B14 — Glossy's build file for the Compose module, `compileSdk` matching the app's, Compose train taken from this repo's version catalog |
| `app/build.gradle.kts` | +18 / −6 | B17 — `BuildConfig.BUILD_STAMP`; also the build-side wiring F1 needed |

### Playback & lyrics

| File | ± | What it carries |
|---|---|---|
| `playback/MusicService.kt` | +74 / −21 | B1 — pause on the automatic track transition when autoplay is off; one retry per run for a song cached as `LYRICS_NOT_FOUND` |
| `spotify/SpotifySession.kt` | +54 / −4 | B2 — `warmUp()` on its own scope so a mint outlives its caller; the cookie key documented for the lyrics race |
| `lyrics/LyricsHelper.kt` | +55 / −2 | B2 — Spotify's own 6 s budget inside the 12 s ceiling, and the mint started *before* the race |
| `lyrics/SpotifyLyricsProvider.kt` | +79 / −35 | B3 + B4 — reads the real payload shape, drops untimed lines, enabled by sign-in until the switch is answered; starts its own warm-up |
| `lyrics/LyricsProviderRegistry.kt` | +12 / −1 | B3 — Spotify leads the default provider order |
| `lyrics/LyricsUtils.kt` | +35 / −1 | F10 — `collapseConsecutiveDuplicates` |
| `viewmodels/HomeViewModel.kt` | +27 / −3 | B10 — the listen history leads Quick picks |

### Sea glass

| File | ± | What it carries |
|---|---|---|
| `ui/component/SeaGlassSupport.kt` | +203 | F2 + B12 + B15 — availability gating, `Modifier.seaGlass`, painted fallback below API 33, hoisted shaders |
| `ui/component/SeaGlassPill.kt` | +194 | F2 + B15 — the shared pill (pastel body, chalky rim, ink-tinted lift shadow) |
| `ui/component/SeaGlassPanel.kt` | +100 | F2 — the section pane every screen seats its lockups on |
| `ui/component/GlossyDesignSystem.kt` | +51 / −20 | F2 — the one material's tokens, replacing the per-screen tunings |
| `ui/component/AppNavigation.kt` | +20 / −3 | F2 — app chrome on the shared material |
| `ui/component/MediaDetailHero.kt` | +13 / −8 | F2 — the hero play button on the shared pill |
| `ui/screens/GlossyLoginScreen.kt` | +119 / −26 | F2 + B12 — login card on the shared pill, glass gated before SeaGlass is reached |
| `ui/screens/WelcomeScreen.kt` | +27 / −13 | F2 — onboarding panes |
| `ui/screens/HomeScreen.kt` | +6 | F2 + B9 — section header on the pane; unused sea-glass import dropped |
| `ui/screens/library/LibraryScreen.kt` | +70 / −60 | F2 — section lockups on the pane |
| `ui/screens/library/LibraryMixScreen.kt` | +6 / −10 | F2 — same, for Mix |

### Mini player, chrome & motion

| File | ± | What it carries |
|---|---|---|
| `ui/player/MiniPlayer.kt` | +378 / −54 | B6, B7, F3, F4 + B15 — the fold is Apple-Music-only, the corner percent is clamped, the tonal background style |
| `ui/component/AppScrollState.kt` | +76 (new) | B8 — `AppScrollState` (consumed deltas only, 24 px threshold) and `LocalAppScrolledToTop` |
| `MainActivity.kt` | +111 / −44 | F4 + F5 — hosts the collapsing chrome and the page-open transitions |
| `ui/player/PlayerOpenBloom.kt` | +69 (new) | F5 — the transition itself |
| `ui/component/BottomSheet.kt` | +11 | F5 — sheet surface aligned with the new page transition |
| `ui/utils/FadingEdge.kt` | +43 / −54 | B15 — gradients built in `drawWithCache` |

### Player, queue & lyrics UI

| File | ± | What it carries |
|---|---|---|
| `ui/player/Queue.kt` | +155 / −69 | F9 + B13 — the redesigned queue header and readable rows |
| `ui/player/applemusic/AppleMusicQueueView.kt` | +4 | B13 — white rows over artwork, as in the rest of the queue |
| `ui/player/Player.kt` | +20 / −1 | F6 — the audio-format badge in the player |
| `ui/component/AudioFormatBadge.kt` | +95 (new) | F6 — the badge |
| `ui/component/Items.kt` | +23 / −2 | B13 — `LocalListRowContentColor`, null by default so ordinary lists are unchanged |
| `ui/component/LyricsCommon.kt` | +24 (new) | F7 — the blur steps (`lyricsLineBlur`), alongside the lyric list model and interval indicator |
| `ui/component/LyricsLine.kt` | +30 / −7 | B9 + F7 — inset applied after the layer; the recede blur |
| `ui/component/OriginalLyrics.kt` | +40 / −7 | B9 + F7 — same two, in the original lyric view |
| `ui/component/ExperimentalLyrics.kt` | +17 / −4 | F7 — recede blur in the experimental view |
| `ui/player/applemusic/NowPlayingContentAppleMusic.kt` | +21 / −16 | B15 — artwork fade hoisted out of the draw pass |
| `ui/component/GlassBackdrop.kt` | +16 / −5 | B15 — backdrop gradients hoisted |

### Settings, theme & resources

| File | ± | What it carries |
|---|---|---|
| `constants/PreferenceKeys.kt` | +84 / −15 | Every new key the branch needs: mini player style, lyric blur, imported font, format badge, page transitions |
| `ui/screens/settings/ContentSettings.kt` | +12 / −1 | B5 — the Spotify lyrics switch defaults to the sign-in state |
| `ui/screens/settings/AppearanceSettings.kt` | +20 / −3 | F3/F4 style choice, F8 entry point, B16 row removed |
| `ui/screens/settings/PlayerSettings.kt` | +47 / −49 | F8 — 36 sp lyric default; B16 rows removed |
| `ui/screens/settings/AppFontSettingsScreen.kt` | +122 / −26 | F8 — the font importer |
| `ui/theme/Type.kt` | +39 | F8 — imported font plumbed into the type scale |
| `ui/theme/Theme.kt` | +7 / −12 | F8 — theme hands the imported font through |
| `res/values/glossy_strings.xml` | +17 / −1 | F5, F6, F8, F9, B16 — the new strings |
| `res/values/metrolist_strings.xml` | +1 / −1 | B11 — the crash report's name |

### Removals

| File | ± | What it carries |
|---|---|---|
| `eq/soundfx/AudioBoost.kt` | −51 (deleted) | B16 — the boost path, removed rather than left inert |
| `ui/component/AudioBoostDialog.kt` | −57 (deleted) | B16 — its dialog |
| `eq/soundfx/SoundFx.kt` | +4 / −23 | B16 — boost hooks out of the sound-fx chain |
| `eq/soundfx/SoundFxRepository.kt` | +2 / −20 | B16 — boost plumbing out of the repository |
| `ui/menu/PlayerMenu.kt` | −32 | B16 — the menu entry removed |

### Preview pages (documentation only, nothing shipped)

| File | ± | What it renders |
|---|---|---|
| `preview/glossy-seaglass-surfaces.html` | +577 | F1/F2 — the glass surfaces |
| `preview/glossy-materialistic-seaglass-and-pill.html` | +493 | F2/F3 — the tonal mini player and the shared pill |
| `preview/glossy-seaglass-everywhere.html` | +410 | F2 — the material applied across screens |
| `preview/glossy-seaglass-onboarding-and-wavy-pill.html` | +383 | F2 — onboarding panes and the wavy pill |
| `preview/glossy-seaglass-android10.html` | +350 | B12 — below-API-33 fallback |
| `preview/glossy-seaglass-app.html` | +321 | F2 + B12 — the sea-glass app page (incl. the below-API-33 pills) |
| `preview/glossy-miniplayer-fold-and-spotify-lyrics.html` | +257 | B3/B6/B7 — the fold fix and the Spotify parse |
| `preview/glossy-apple-music-mini-player-style.html` | +229 | F4 — the Apple Music bar and its fold |
| `preview/glossy-lyrics-blur-and-custom-font.html` | +211 | F7/F8 — lyric blur and the font import |
| `preview/glossy-spotlight-and-lyrics-fixes.html` | +208 | B9 + the spotlight fixes |
| `preview/glossy-miniplayer-pill-diagnosis.html` | +88 | B6/B7 — the pill geometry measured against each style |
| `preview/glossy-android10-queue-and-popup.html` | +639 | F6/F9 + B12 — queue, popup and the old-device path |

---

### Full paths, verbatim — all 69

The tables above name files by their position under `app/src/main/kotlin/com/jay/glossy/` (or under `preview/`) to keep the rows readable. Every changed path, exactly as this PR's *Files changed* tab lists it, is here — `A` added, `M` modified, `D` deleted, with this branch's line counts:

```
A +  76 -0    app/src/main/kotlin/com/jay/glossy/ui/component/AppScrollState.kt
A +  95 -0    app/src/main/kotlin/com/jay/glossy/ui/component/AudioFormatBadge.kt
A + 100 -0    app/src/main/kotlin/com/jay/glossy/ui/component/SeaGlassPanel.kt
A + 194 -0    app/src/main/kotlin/com/jay/glossy/ui/component/SeaGlassPill.kt
A + 203 -0    app/src/main/kotlin/com/jay/glossy/ui/component/SeaGlassSupport.kt
A +  69 -0    app/src/main/kotlin/com/jay/glossy/ui/player/PlayerOpenBloom.kt
A + 639 -0    preview/glossy-android10-queue-and-popup.html
A + 229 -0    preview/glossy-apple-music-mini-player-style.html
A + 211 -0    preview/glossy-lyrics-blur-and-custom-font.html
A + 493 -0    preview/glossy-materialistic-seaglass-and-pill.html
A + 257 -0    preview/glossy-miniplayer-fold-and-spotify-lyrics.html
A +  88 -0    preview/glossy-miniplayer-pill-diagnosis.html
A + 350 -0    preview/glossy-seaglass-android10.html
A + 321 -0    preview/glossy-seaglass-app.html
A + 410 -0    preview/glossy-seaglass-everywhere.html
A + 383 -0    preview/glossy-seaglass-onboarding-and-wavy-pill.html
A + 577 -0    preview/glossy-seaglass-surfaces.html
A + 208 -0    preview/glossy-spotlight-and-lyrics-fixes.html
A +  53 -0    seaglass-build/seaglass-compose.gradle.kts
A +  42 -0    seaglass-build/seaglass-core.gradle.kts
A +   1 -0    third_party/SeaGlass
M +   3 -0    .gitmodules
M +  18 -6    app/build.gradle.kts
M + 111 -44   app/src/main/kotlin/com/jay/glossy/MainActivity.kt
M +  84 -15   app/src/main/kotlin/com/jay/glossy/constants/PreferenceKeys.kt
M +   4 -23   app/src/main/kotlin/com/jay/glossy/eq/soundfx/SoundFx.kt
M +   2 -20   app/src/main/kotlin/com/jay/glossy/eq/soundfx/SoundFxRepository.kt
M +  55 -2    app/src/main/kotlin/com/jay/glossy/lyrics/LyricsHelper.kt
M +  12 -1    app/src/main/kotlin/com/jay/glossy/lyrics/LyricsProviderRegistry.kt
M +  35 -1    app/src/main/kotlin/com/jay/glossy/lyrics/LyricsUtils.kt
M +  79 -35   app/src/main/kotlin/com/jay/glossy/lyrics/SpotifyLyricsProvider.kt
M +  74 -21   app/src/main/kotlin/com/jay/glossy/playback/MusicService.kt
M +  54 -4    app/src/main/kotlin/com/jay/glossy/spotify/SpotifySession.kt
M +  20 -3    app/src/main/kotlin/com/jay/glossy/ui/component/AppNavigation.kt
M +  11 -0    app/src/main/kotlin/com/jay/glossy/ui/component/BottomSheet.kt
M +  17 -4    app/src/main/kotlin/com/jay/glossy/ui/component/ExperimentalLyrics.kt
M +  16 -5    app/src/main/kotlin/com/jay/glossy/ui/component/GlassBackdrop.kt
M +  51 -20   app/src/main/kotlin/com/jay/glossy/ui/component/GlossyDesignSystem.kt
M +  23 -2    app/src/main/kotlin/com/jay/glossy/ui/component/Items.kt
M +  24 -0    app/src/main/kotlin/com/jay/glossy/ui/component/LyricsCommon.kt
M +  30 -7    app/src/main/kotlin/com/jay/glossy/ui/component/LyricsLine.kt
M +  13 -8    app/src/main/kotlin/com/jay/glossy/ui/component/MediaDetailHero.kt
M +  40 -7    app/src/main/kotlin/com/jay/glossy/ui/component/OriginalLyrics.kt
M +   0 -32   app/src/main/kotlin/com/jay/glossy/ui/menu/PlayerMenu.kt
M + 378 -54   app/src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt
M +  20 -1    app/src/main/kotlin/com/jay/glossy/ui/player/Player.kt
M + 155 -69   app/src/main/kotlin/com/jay/glossy/ui/player/Queue.kt
M +   4 -0    app/src/main/kotlin/com/jay/glossy/ui/player/applemusic/AppleMusicQueueView.kt
M +  21 -16   app/src/main/kotlin/com/jay/glossy/ui/player/applemusic/NowPlayingContentAppleMusic.kt
M +   1 -1    app/src/main/kotlin/com/jay/glossy/ui/screens/CrashActivity.kt
M + 119 -26   app/src/main/kotlin/com/jay/glossy/ui/screens/GlossyLoginScreen.kt
M +   6 -0    app/src/main/kotlin/com/jay/glossy/ui/screens/HomeScreen.kt
M +  27 -13   app/src/main/kotlin/com/jay/glossy/ui/screens/WelcomeScreen.kt
M +   6 -10   app/src/main/kotlin/com/jay/glossy/ui/screens/library/LibraryMixScreen.kt
M +  70 -60   app/src/main/kotlin/com/jay/glossy/ui/screens/library/LibraryScreen.kt
M + 122 -26   app/src/main/kotlin/com/jay/glossy/ui/screens/settings/AppFontSettingsScreen.kt
M +  20 -3    app/src/main/kotlin/com/jay/glossy/ui/screens/settings/AppearanceSettings.kt
M +  12 -1    app/src/main/kotlin/com/jay/glossy/ui/screens/settings/ContentSettings.kt
M +  47 -49   app/src/main/kotlin/com/jay/glossy/ui/screens/settings/PlayerSettings.kt
M +   7 -12   app/src/main/kotlin/com/jay/glossy/ui/theme/Theme.kt
M +  39 -0    app/src/main/kotlin/com/jay/glossy/ui/theme/Type.kt
M +  43 -54   app/src/main/kotlin/com/jay/glossy/ui/utils/FadingEdge.kt
M +  25 -5    app/src/main/kotlin/com/jay/glossy/utils/YTPlayerUtils.kt
M +  27 -3    app/src/main/kotlin/com/jay/glossy/viewmodels/HomeViewModel.kt
M +  17 -1    app/src/main/res/values/glossy_strings.xml
M +   1 -1    app/src/main/res/values/metrolist_strings.xml
M +  34 -0    settings.gradle.kts
D +   0 -51   app/src/main/kotlin/com/jay/glossy/eq/soundfx/AudioBoost.kt
D +   0 -57   app/src/main/kotlin/com/jay/glossy/ui/component/AudioBoostDialog.kt
```

## 4. How to check it

```bash
git clone https://github.com/izybro110-gif/Glossy && cd Glossy
git checkout fix/autoplay-stop-and-spotify-lyrics
git submodule update --init --recursive      # required: third_party/SeaGlass is not optional
./gradlew :app:assembleFossDebug             # or the variant you build
```

The submodule is the only new setup step: `settings.gradle.kts` fails with that exact command if it is missing.

### Manual checklist per bug

1. **B1** — turn Autoplay off, let a track end: the player pauses; queue, notification and position are untouched; turn it on and playback resumes the behaviour you had.
2. **B2/B3/B4/B5** — sign in to Spotify, play a song Spotify has lyrics for: lyrics appear; Content settings reads *Spotify lyrics: on*; switch it off and they stop being fetched.
3. **B6/B7** — check all mini player styles while scrolling: only Apple Music folds, nothing crashes, Studio/Special Edition/Legacy/Modern keep their shape.
4. **B8** — a small nudge no longer collapses the chrome; a deliberate swipe does; scrolling back up expands it.
5. **B9/F7** — no first-letter clipping on any line; unsung lines recede; on Android 11 and below the recede is alpha only.
6. **B10** — Quick picks leads with what you have been playing.
7. **B11** — trigger a crash report: it is titled Glossy and named `glossy_crash_*`-style.
8. **B12** — on an Android 10 device: login screen, onboarding and the sea-glass surfaces draw the painted fallback, no `NoClassDefFoundError`.
9. **B13** — the queue is readable over a saturated cover.
10. **B17** — the foot of Settings shows the build stamp with the SHA CI built.

---

## 5. What was verified, and what was not

**Verified while building it**
- The fold arithmetic: the low-bouncy spring's overshoot (≈ −1.4) and the clamped replacement, drawn side by side in `preview/glossy-miniplayer-pill-diagnosis.html`.
- The scroll threshold: offered-vs-consumed deltas and the resulting 12 px → 24 px behaviour, in `preview/glossy-miniplayer-fold-and-spotify-lyrics.html`.
- The lyric clip: located by pixel scan on a rendered line, before and after, in `preview/glossy-spotlight-and-lyrics-fixes.html`.
- The Spotify payload shape: checked against the endpoint's real JSON (`lyrics.lines`, `startTimeMs` as a string, `words` as text) rather than assumed.
- The below-API-33 path: `android.graphics.RuntimeShader` does not exist below 33, so the gate is placed before the class is touched, not after.
- The one non-Kotlin build claim: `checkAarMetadata` was the reported failure, and the Compose dependencies' required `compileSdk` is what the module now matches.

**Not verified — please weigh these in review**
- **Nothing on this branch was compiled locally.** The machine this was authored on has no JDK and no Android SDK, so the first real compile of these commits is CI's. Type-checking of the Kotlin, resource merging, and the Room/Compose annotation passes are all unproven until then.
- **No on-device playback run.** B1, B2 and B12 are reasoned from code paths and the platform APIs, not from a phone: the autoplay pause, the token mint's survival across a lookup, and the Android 10 fallback all still want one device pass each.
- The Spotify lyrics fixes (B2–B5) could not be exercised against a real signed-in Spotify account here.

---

## 6. Review notes

- **The branch is large because it is the whole delta from 0.0.4**, not because it is one change. If you would rather take it in pieces, the commit order already groups cleanly: `2a1db051` → `05248b8e` → `e90e4a67` → `4a5dd726` → `385d9dbc` → `2cf1abac` → `d05e3f11` → `06c0fb3c` → `05c29599` → `0dfad4b9` → `26bed8c2` → `0c6aba16` → `ee037e8c` → `e2979189` → `82842840` → `eb76c226` → `d139c41c`. The sea-glass work (settings + submodule + `SeaGlass*`) and the mini player work (`MiniPlayer.kt` + `MainActivity.kt`) are the only two places where a partial merge would not compile on its own — those two want to land together.
- **A new submodule is the biggest thing to accept here** (F1). It is public, pinned, and the alternative — shipping SeaGlass as an artifact — does not exist because upstream publishes none.
- **Version is deliberately left at `versionCode 4 / versionName "0.0.4"`**; bump it whenever you cut the next release.
- `preview/*.html` is 4166 of the 6976 insertions and none of it is inside the APK — if you would rather keep them out of `main`, they can be dropped from this branch without touching a line of app code.

🤖 Generated with [Codebuff](https://codebuff.com)
