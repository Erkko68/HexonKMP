package eric.bitria.hexon.data.remote

import co.touchlab.kermit.Logger
import eric.bitria.hexon.config.EnvConfig
import eric.bitria.hexon.ws.GameMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.http.Url
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val TAG = "GameSocketClient"

interface GameSocketClient {
    suspend fun connect(sessionId: String): DefaultWebSocketSession
    fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage>
    suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage)
}

class KtorGameSocketClient(
    private val client: HttpClient
) : GameSocketClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    override suspend fun connect(sessionId: String): DefaultWebSocketSession {
        Logger.d(TAG) { "connect() called with sessionId: $sessionId" }
        val session = client.webSocketSession {
            url {
                val base = Url(EnvConfig.BASE_URL)
                protocol = if (base.protocol.name == "https") URLProtocol.WSS else URLProtocol.WS
                host = base.host
                port = base.port
                path("game", sessionId)
                Logger.d(TAG) { "WebSocket URL: ${protocol.name}://${host}:${port}/game/${sessionId}" }
            }
        }
        Logger.d(TAG) { "WebSocket connection established successfully" }
        return session
    }

    override fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage> {
        Logger.d(TAG) { "observeMessages() started" }
        return session.incoming.consumeAsFlow()
            .map { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val rawText = frame.readText()

                        // Filter out empty frames (WebSocket keep-alive, etc.)
                        if (rawText.isBlank()) {
                            Logger.d(TAG) { "Received empty frame, skipping" }
                            return@map null
                        }

                        try {
                            val message = json.decodeFromString<GameMessage>(rawText)
                            Logger.d(TAG) { "Received: ${message::class.simpleName}" }
                            message
                        } catch (e: Exception) {
                            Logger.e(TAG, e) { "Failed to decode message: ${rawText.take(100)}" }
                            throw e
                        }
                    }
                    is Frame.Ping, is Frame.Pong -> {
                        Logger.d(TAG) { "Received ${frame::class.simpleName}" }
                        null
                    }
                    else -> {
                        Logger.d(TAG) { "Received ${frame::class.simpleName}, ignoring" }
                        null
                    }
                }
            }
            .filterNotNull()
    }

    override suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage) {
        Logger.d(TAG) { "sendMessage() called with message type: ${message::class.simpleName}" }
        val text = json.encodeToString(message)
        Logger.d(TAG) { "Encoded JSON length: ${text.length}" }
        Logger.d(TAG) { "Sending JSON: $text" }
        session.send(Frame.Text(text))
        Logger.d(TAG) { "Message sent successfully" }
    }
}
