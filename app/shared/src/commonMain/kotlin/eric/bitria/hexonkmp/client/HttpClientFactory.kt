package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.config.EnvConfig
import eric.bitria.hexonkmp.core.protocol.RegisterRequest
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

expect fun createHttpClient(prefs: DevicePreferences): HttpClient

fun HttpClientConfig<*>.commonConfig(prefs: DevicePreferences) {
    install(ContentNegotiation) { json(AppJson) }
    install(WebSockets)
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 10_000
    }
    // Central auth: attach the stored bearer token to every request and, on a 401,
    // re-register (our token "refresh") and retry — in one place, not per call.
    install(Auth) {
        bearer {
            loadTokens { prefs.getToken()?.let { BearerTokens(it, it) } }
            refreshTokens {
                val name = prefs.getPlayerName() ?: return@refreshTokens null
                val response: RegisterResponse = client.post("/register") {
                    markAsRefreshTokenRequest() // don't recurse through this same auth
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest(name = name, token = prefs.getToken()))
                }.body()
                prefs.setToken(response.token)
                prefs.setPlayerId(response.playerId)
                BearerTokens(response.token, response.token)
            }
            // Attach the bearer header proactively to the HTTP API calls. Excluded:
            //  - /register: mints/refreshes tokens, carries its own in the body.
            //  - /game/<id>: the WebSocket handshake — browsers can't set headers on
            //    it, so the WS authenticates with a ?token= query param instead.
            sendWithoutRequest { request ->
                val path = request.url.encodedPath
                !path.startsWith("/register") && !path.startsWith("/game/")
            }
        }
    }
    defaultRequest {
        // Protocol drives TLS: HTTPS here makes the WebSockets plugin upgrade to
        // WSS too. Without it Ktor defaults to plaintext http/ws.
        url.protocol = if (EnvConfig.SECURE) URLProtocol.HTTPS else URLProtocol.HTTP
        host = EnvConfig.SERVER_HOST
        port = EnvConfig.SERVER_PORT
        contentType(ContentType.Application.Json)
    }
}
