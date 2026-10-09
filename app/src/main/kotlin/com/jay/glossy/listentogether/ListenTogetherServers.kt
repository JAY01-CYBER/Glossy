/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Server registry for Listen Together. The list is seeded with the known
 * endpoints and refreshed at startup from Echo Music's published server.json,
 * so the default room server keeps working when operators move instances —
 * a hardcoded URL silently dies with its host.
 *
 * Ported from Echo Music's ListenTogetherServers (GPL-3.0).
 */

package com.jay.glossy.listentogether

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    /** Echo Music's published server descriptor: { name, serverUrl, region }. */
    private const val SERVER_JSON_URL =
        "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/refs/heads/main/app/server.json"

    /**
     * Glossy's own Cloudflare Worker + Durable Object deployment, committed in
     * `metroserver-worker/` and published by `npx wrangler deploy`. It is the
     * first preset because it is the one the app's own client is verified
     * against; the Echo Music refresh below never displaces it.
     */
    const val GLOSSY_CLOUDFLARE_URL = "wss://glossy-listen-together.izybro110.workers.dev/ws"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()

    private val _servers = MutableStateFlow(
        listOf(
            // Glossy's own deployment — always first, always present.
            ListenTogetherServer(
                name = "Glossy on Cloudflare",
                url = GLOSSY_CLOUDFLARE_URL,
                location = "Cloudflare edge",
                operator = "Glossy",
            ),
            // Live server published by Echo Music; refreshed asynchronously below.
            ListenTogetherServer(
                name = "Metrolist",
                url = "wss://metroserverx.meowery.eu/ws",
                location = "Poland",
                operator = "Metrolist",
            ),
            // Glossy's original endpoint — kept so a saved selection and older
            // room links still resolve even if the refresh never lands.
            ListenTogetherServer(
                name = "The Meowery",
                url = "wss://rx.meowery.eu/ws",
                location = "Poland",
                operator = "Nyx",
            ),
        ),
    )

    val serversFlow: StateFlow<List<ListenTogetherServer>> = _servers.asStateFlow()

    val servers: List<ListenTogetherServer>
        get() = _servers.value

    init {
        scope.launch {
            try {
                val request = Request.Builder().url(SERVER_JSON_URL).build()
                val response = client.newCall(request).execute()
                val body = response.body.string()
                val jsonObject = Json.parseToJsonElement(body).jsonObject
                val name = jsonObject["name"]?.jsonPrimitive?.content ?: "Metrolist Server"
                val url = jsonObject["serverUrl"]?.jsonPrimitive?.content
                    ?: "wss://metroserverx.meowery.eu/ws"
                val region = jsonObject["region"]?.jsonPrimitive?.content ?: "Poland"

                val discovered = ListenTogetherServer(
                    name = name,
                    url = url,
                    location = region,
                    operator = "",
                )
                // Keep Glossy's own Cloudflare deployment pinned to the front: the
                // refresh adds the discovered server but never displaces it.
                val current = _servers.value
                val withoutDuplicate = current.filterNot { it.url == discovered.url }
                val glossy = withoutDuplicate.firstOrNull { it.url == GLOSSY_CLOUDFLARE_URL }
                val rest = withoutDuplicate.filterNot { it.url == GLOSSY_CLOUDFLARE_URL }
                _servers.value =
                    if (glossy != null) {
                        listOf(glossy) + listOf(discovered) + rest
                    } else {
                        listOf(discovered) + rest
                    }
            } catch (_: Exception) {
                // Keep the seeded list — fallback implicitly retained.
            }
        }
    }

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }

    /**
     * A `*.workers.dev` host carries the account label the Cloudflare subdomain
     * was minted from, which is the deployer's own handle and does not belong in
     * a screenshot. Every display in the UI goes through this, so the account
     * label never leaves the address field:
     * `glossy-listen-together.account.workers.dev`.
     */
    fun maskAccountLabel(host: String): String {
        val labels = host.split(".")
        val suffix = "workers.dev"
        if (labels.size < 3 || labels.takeLast(2).joinToString(".") != suffix) {
            return host
        }
        val masked = labels.toMutableList()
        masked[masked.size - 3] = "account"
        return masked.joinToString(".")
    }

    /** The host as the UI may show it — masked for a Cloudflare deployment. */
    fun serverHostLabel(url: String): String {
        val host = hostOf(url) ?: return url
        return maskAccountLabel(host)
    }

    /** The whole address with the account label masked, for lines that show the URL. */
    fun serverAddressLabel(url: String): String {
        val normalised = normaliseServerUrl(url)
        val host = hostOf(url) ?: return url
        return normalised.replaceFirst(host, maskAccountLabel(host))
    }

    /** What a status row or list calls a server: its preset name, else the masked host. */
    fun serverDisplayName(url: String): String {
        val normalised = normaliseServerUrl(url).lowercase()
        servers.firstOrNull { normaliseServerUrl(it.url).lowercase() == normalised }?.let {
            return it.name
        }
        return serverHostLabel(url)
    }

    /**
     * Accepts what a person actually types. A bare `host:port` used to reach
     * OkHttp and throw out of the tap handler; here it becomes `ws://…`, a
     * pasted `https://…` becomes `wss://…`, and a missing path becomes `/ws`.
     */
    fun normaliseServerUrl(raw: String): String {
        var value = raw.trim()
        if (value.isEmpty()) return value

        value =
            when {
                value.startsWith("https://") -> "wss://" + value.removePrefix("https://")
                value.startsWith("http://") -> "ws://" + value.removePrefix("http://")
                !value.contains("://") -> "ws://$value"
                else -> value
            }

        while (value.endsWith("/")) {
            value = value.dropLast(1)
        }

        val host = hostOf(value)
        if (host.isNullOrEmpty()) return raw.trim()
        val path = value.substringAfter("://").removePrefix(host)
        return if (path.isEmpty() || path == "/") "$value/ws" else value
    }

    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null
        val hostPort = afterScheme.substringBefore('/').substringBefore('?')
        return hostPort.substringBefore(':').ifEmpty { null }
    }
}
