/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

/**
 * Whether this device can run a SeaGlass surface at all.
 *
 * Pinned off: the frosted-glass surfaces never shipped as the look people
 * asked for — the flat button and pill drawn below API 33 was the one testers
 * kept picking — so every call site now takes its flat branch on every device,
 * and all of them already had that branch for API < 33.
 *
 * The constant stays a named read-only value rather than a hardcoded `false` at
 * each call site so a future reintroduction is one edit, and so the tier checks
 * inside SeaGlass stay unreachable without deleting the dependency.
 */
val seaGlassSurfacesAvailable: Boolean = false
