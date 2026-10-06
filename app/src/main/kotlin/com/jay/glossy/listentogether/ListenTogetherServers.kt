/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
<<<<<<< HEAD
 *
 * Server registry for Listen Together. The list is seeded with the known
 * endpoints and refreshed at startup from Echo Music's published server.json,
 * so the default room server keeps working when operators move instances —
 * a hardcoded URL silently dies with its host.
 *
 * Ported from Echo Music's ListenTogetherServers (GPL-3.0).
=======
>>>>>>> origin/main
 */

package com.jay.glossy.listentogether

<<<<<<< HEAD
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
=======
import com.jay.glossy.R

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
>>>>>>> origin/main

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
<<<<<<< HEAD
    /** Echo Music's published server descriptor: { name, serverUrl, region }. */
    private const val SERVER_JSON_URL =
        "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/refs/heads/main/app/server.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()

    private val _servers = MutableStateFlow(
        listOf(
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
                _servers.value = listOf(discovered) + _servers.value.filterNot { it.url == discovered.url }
            } catch (_: Exception) {
                // Keep the seeded list — fallback implicitly retained.
            }
        }
=======
    private const val ServersJson = """
        [
          {
            "name": "The Meowery",
            "url": "wss://rx.meowery.eu/ws",
            "location": "Poland",
            "operator": "Nyx"
          }
        ]
    """

    private val json = Json { ignoreUnknownKeys = true }

    val servers: List<ListenTogetherServer> by lazy {
        json.decodeFromString(ServersJson)
>>>>>>> origin/main
    }

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}
