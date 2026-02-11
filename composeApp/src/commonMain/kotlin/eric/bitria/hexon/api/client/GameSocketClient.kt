package eric.bitria.hexon.api.client

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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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
        return client.webSocketSession {
            url {
                val base = Url(EnvConfig.BASE_URL)
                protocol = if (base.protocol.name == "https") URLProtocol.WSS else URLProtocol.WS
                host = base.host
                port = base.port
                path("game", sessionId)
            }
        }
    }

    override fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage> {
        return session.incoming.consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .map { frame ->
                json.decodeFromString<GameMessage>(frame.readText())
            }
    }

    override suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage) {
        val text = json.encodeToString(message)
        session.send(Frame.Text(text))
    }
}
