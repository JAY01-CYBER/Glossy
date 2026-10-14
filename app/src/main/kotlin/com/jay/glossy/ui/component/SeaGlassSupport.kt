/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import android.os.Build

/**
 * Whether this device can run a SeaGlass surface at all.
 *
 * SeaGlass's surface pass resolves `android.graphics.RuntimeShader`, which only
 * exists from Android 13 (API 33). Below that the resolution fails the moment
 * the pass runs — inside `Modifier.seaGlass`, before any of SeaGlass's own tier
 * checks can fall back to a software path — and the app dies with
 * `NoClassDefFoundError` instead of degrading. The guard therefore has to sit
 * outside SeaGlass: below API 33 every Glossy call site paints its own flat
 * surface rather than asking for glass.
 *
 * Read once, because `SDK_INT` cannot change while the process lives.
 */
val seaGlassSurfacesAvailable: Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
