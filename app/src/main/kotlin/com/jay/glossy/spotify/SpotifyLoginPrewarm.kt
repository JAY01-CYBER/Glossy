/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.jay.glossy.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

internal const val SPOTIFY_LOGIN_URL =
    "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
internal const val SPOTIFY_WEBVIEW_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"

/**
 * Builds the shared login WebView: application context (safe to outlive the
 * screen), browser user agent (Spotify bot-checks the default WebView UA),
 * and the login URL already loading.
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun newSpotifyLoginWebView(context: Context): WebView =
    WebView(context).apply { // Changed applicationContext to just context
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = SPOTIFY_WEBVIEW_USER_AGENT
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.blockNetworkImage = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = WebViewClient()
        loadUrl(SPOTIFY_LOGIN_URL)
    }

/**
 * Heads the login page load off: [warm] starts fetching open.spotify.com's
 * login flow before the user taps through, and [park] keeps the WebView (and
 * its loaded page) alive when the screen closes so reopening the screen is
 * instant instead of a fresh multi-second page load. Main thread only; one
 * instance max.
 */
object SpotifyLoginPrewarm {
    private var pending: WebView? = null

    @JvmStatic
    fun warm(context: Context) {
        if (pending != null) return
        pending = newSpotifyLoginWebView(context)
    }

    /** The prewarmed WebView (removed from the cache), or null. */
    @JvmStatic
    fun take(): WebView? = pending.also { pending = null }

    /** Keep a screen's WebView for the next visit instead of destroying it. */
    @JvmStatic
    fun park(webView: WebView) {
        val previous = pending
        pending = webView
        if (previous != null && previous !== webView) destroy(previous)
        runCatching { (webView.parent as? ViewGroup)?.removeView(webView) }
    }

    private fun destroy(webView: WebView) {
        runCatching {
            webView.stopLoading()
            webView.destroy()
        }
    }
}
