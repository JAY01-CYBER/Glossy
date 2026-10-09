/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Turns a Listen Together failure into a sentence the user can act on.
 *
 * The client reports what the server said, which is precise but written for
 * operators: "host_not_allowed — Only allowlisted clients can host rooms" tells
 * someone who just tapped Create room nothing about what to do next. The codes
 * the app can explain itself are translated here; anything else keeps the
 * server's own wording, so an unknown failure is still visible instead of
 * silent.
 */

package com.jay.glossy.ui.utils

import android.content.Context
import com.jay.glossy.R
import com.jay.glossy.listentogether.ErrorCodes

/**
 * Text for an error the server sent back, keyed by its code.
 */
fun listenTogetherServerErrorText(
    context: Context,
    code: String?,
    message: String,
): String =
    when (code) {
        ErrorCodes.HOST_NOT_ALLOWED -> context.getString(R.string.listen_together_host_not_allowed)
        else -> message
    }

/**
 * Text for a failure the client hit on its own — the socket never opened, or it
 * gave up after exhausting its reconnect attempts. The exception text is left
 * to the connection log, which the settings screen exposes.
 */
fun listenTogetherConnectionErrorText(context: Context): String =
    context.getString(R.string.listen_together_connection_failed)
