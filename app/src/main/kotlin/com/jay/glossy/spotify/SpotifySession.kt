package com.jay.glossy.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object SpotifySession {
    private const val BRIDGE_NAME = "GlossySpotifyTokenBridge"
    private const val HARVEST_TIMEOUT_MS = 20_000L
    private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L

    private val cookieKey = stringPreferencesKey("spotify_sp_dc")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()

    @Volatile private var cached: Token? = null
    @Volatile private var cachedSession: SessionInfo? = null
    @Volatile private var cachedClientToken: String? = null
    @Volatile private var clientTokenExpiresAtMs = 0L

    suspend fun cookie(context: Context): String = context.dataStore.data.first()[cookieKey].orEmpty()

    suspend fun saveCookie(context: Context, value: String) {
        context.safeDataStoreEdit { preferences -> preferences[cookieKey] = value }
        cached = null
    }

    suspend fun clear(context: Context) {
        context.safeDataStoreEdit { preferences -> preferences.remove(cookieKey) }
        cached = null
    }

    /**
     * Login: save a pasted sp_dc token, then prove it works by
     * minting a real access token with it before committing. Returns a
     * human-readable status string for the UI.
     */
    suspend fun saveAndValidateToken(context: Context, token: String): String {
        if (token.length < 20) return "That doesn't look like an sp_dc token"
        saveCookie(context, token)
        return if (token(context) != null) "Spotify connected"
        else {
            clear(context)
            "Token rejected — check the sp_dc value and try again"
        }
    }

    /**
     * The current bearer token minted from the sp_dc cookie, or null when no
     * cookie is saved or the mint failed. Cached until shortly before expiry.
     *
     * Spotify's self-signed /api/token request (computing the TOTP ourselves)
     * returns 200 with a token, but api.spotify.com and spclient turn that
     * forged token away with 429 — so we load the real web
     * player in an offscreen WebView with the cookie applied and read the
     * /api/token response the page mints for itself (see [harvestViaWebView]).
     */
    suspend fun token(context: Context): Token? = mutex.withLock {
        val cookie = cookie(context).takeIf(String::isNotBlank) ?: return@withLock null
        cached?.takeIf { it.accessTokenExpirationTimestampMs > System.currentTimeMillis() + 30_000 }
            ?.let { return@withLock it }
        try {
            val appContext = context.applicationContext
            val harvested = withContext(Dispatchers.Main) { harvestViaWebView(appContext, cookie) }
            harvested?.also { cached = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads open.spotify.com for real in an offscreen WebView with the
     * listener's cookie applied and reads the token the page mints for itself
     * by hooking fetch/XHR before its own bundle runs. Rejects the anonymous
     * token the player mints before the cookie takes effect — that one can't
     * read canvases, so we keep waiting for the logged-in one.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun harvestViaWebView(context: Context, cookie: String): Token? {
        val deferred = CompletableDeferred<Token?>()

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://open.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://accounts.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            flush()
        }
        // The player caches its token in web storage and skips a fresh
        // /api/token request if a live one is already sitting there, leaving
        // the hook with nothing to see — wipe storage so every harvest forces
        // a real mint.
        runCatching { WebStorage.getInstance().deleteAllData() }

        var webView: WebView? = null
        return try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = USER_AGENT
                cookieManager.setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(TokenBridge(deferred), BRIDGE_NAME)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        // Re-assert in case the player navigated client-side
                        // after the onPageStarted injection ran.
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }
                }
                loadUrl("https://open.spotify.com/")
            }
            withTimeoutOrNull(HARVEST_TIMEOUT_MS) { deferred.await() }
        } catch (e: CancellationException) {
            // Propagate — a harvest cancelled by a track change must not
            // complete and hand back a stale token mid-flight.
            throw e
        } catch (e: Exception) {
            null
        } finally {
            runCatching {
                webView?.removeJavascriptInterface(BRIDGE_NAME)
                webView?.stopLoading()
                webView?.destroy()
            }
        }
    }

    /** Receives raw /api/token response bodies from the hooked page. */
    private class TokenBridge(private val deferred: CompletableDeferred<Token?>) {
        @JavascriptInterface
        fun onTokenPayload(payload: String?) {
            if (payload.isNullOrBlank() || deferred.isCompleted) return
            runCatching {
                val root = json.parseToJsonElement(payload).jsonObject
                val token = root["accessToken"]?.jsonPrimitive?.contentOrNull
                val anonymous = root["isAnonymous"]?.jsonPrimitive?.contentOrNull
                    ?.toBooleanStrictOrNull() ?: false
                if (token.isNullOrBlank() || anonymous) return
                val expiresAt = root["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.contentOrNull
                    ?.toLongOrNull()?.takeIf { it > System.currentTimeMillis() }
                    ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
                val clientId = root["clientId"]?.jsonPrimitive?.contentOrNull.orEmpty()
                deferred.complete(Token(token, expiresAt, clientId, false))
            }
        }
    }

    /**
     * The second header these endpoints demand alongside the bearer token —
     * api.spotify.com and spclient both turn away a request that only carries
     * [Token.accessToken] with a 429. Best-effort: returns null when the
     * client id isn't known yet or minting fails, and callers just send the
     * bearer alone.
     */
    suspend fun clientToken(): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.takeIf { now < clientTokenExpiresAtMs - 30_000 }?.let { return it }
        val clientId = cached?.clientId?.takeIf(String::isNotBlank) ?: return null
        val session = session() ?: return null

        val payload = buildJsonObject {
            putJsonObject("client_data") {
                put("client_version", session.clientVersion)
                put("client_id", clientId)
                putJsonObject("js_sdk_data") {
                    put("device_brand", "unknown")
                    put("device_model", "unknown")
                    put("os", "android")
                    put("os_version", Build.VERSION.RELEASE)
                    put("device_id", session.deviceId)
                    put("device_type", "smartphone")
                }
            }
        }.toString()

        return runCatching {
            val connection = URL("https://clienttoken.spotify.com/v1/clienttoken").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.doOutput = true
                // Exact "application/json" — clienttoken 400s on the
                // charset-suffixed header some serializers send.
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode !in 200..299) return@runCatching null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                if (root["response_type"]?.jsonPrimitive?.contentOrNull != "RESPONSE_GRANTED_TOKEN_RESPONSE") {
                    return@runCatching null
                }
                val granted = root["granted_token"]?.jsonObject ?: return@runCatching null
                val token = granted["token"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val ttlSeconds = granted["expires_after_seconds"]?.jsonPrimitive?.contentOrNull
                    ?.toLongOrNull() ?: 3600L
                clientTokenExpiresAtMs = System.currentTimeMillis() + ttlSeconds * 1000
                token
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.also { cachedClientToken = it }
    }

    /**
     * [SessionInfo.clientVersion] and [SessionInfo.deviceId] both come off the
     * plain, unauthenticated web player page — the client version from a
     * base64 JSON blob it embeds for itself, the device id from the sp_t
     * cookie it hands out to every visitor. Neither changes within a session.
     */
    suspend fun session(): SessionInfo? {
        cachedSession?.let { return it }
        val result = runCatching {
            val connection = URL("https://open.spotify.com").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) return@runCatching null
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val deviceId = connection.headerFields.orEmpty()["Set-Cookie"].orEmpty()
                    .firstNotNullOfOrNull { header ->
                        Regex("sp_t=([^;]+)").find(header)?.groupValues?.get(1)
                    }
                val configB64 = Regex("<script id=\"appServerConfig\" type=\"text/plain\">([^<]+)</script>")
                    .find(html)?.groupValues?.get(1) ?: return@runCatching null
                val configJson = String(java.util.Base64.getDecoder().decode(configB64), Charsets.UTF_8)
                val clientVersion = json.parseToJsonElement(configJson).jsonObject["clientVersion"]
                    ?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                SessionInfo(clientVersion, deviceId ?: UUID.randomUUID().toString())
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
        return result?.also { cachedSession = it }
    }

    data class SessionInfo(val clientVersion: String, val deviceId: String)

    @Serializable
    data class Token(
        val accessToken: String,
        val accessTokenExpirationTimestampMs: Long,
        val clientId: String = "",
        val isAnonymous: Boolean = false,
    )

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"

    private val HOOK_SCRIPT = """
        (function () {
          if (window.__glossyTokenHook) return;
          window.__glossyTokenHook = true;
          var report = function (body) {
            try { $BRIDGE_NAME.onTokenPayload(body); } catch (e) {}
          };
          var isToken = function (u) {
            try { return String(u).indexOf('/api/token') !== -1; } catch (e) { return false; }
          };
          var origFetch = window.fetch;
          if (origFetch) {
            window.fetch = function (input, init) {
              var url = (input && input.url) ? input.url : input;
              var result = origFetch.apply(this, arguments);
              if (isToken(url)) {
                try {
                  result.then(function (res) {
                    res.clone().text().then(report).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              }
              return result;
            };
          }
          var origOpen = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__glossyUrl = url;
            return origOpen.apply(this, arguments);
          };
          var origSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.send = function () {
            var xhr = this;
            try {
              xhr.addEventListener('load', function () {
                if (isToken(xhr.__glossyUrl)) {
                  try { report(xhr.responseText); } catch (e) {}
                }
              });
            } catch (e) {}
            return origSend.apply(this, arguments);
          };
        })();
    """.trimIndent()
}
