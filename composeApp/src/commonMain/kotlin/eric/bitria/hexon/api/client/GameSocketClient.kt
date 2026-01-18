package eric.bitria.hexon.api.client

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.SessionManager
import eric.bitria.hexon.ws.GameMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface GameSocketClient {
    suspend fun connect(sessionId: String): DefaultWebSocketSession
    fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage>
    suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage)
}

class KtorGameSocketClient(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) : GameSocketClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    override suspend fun connect(sessionId: String): DefaultWebSocketSession {
        val host = BuildKonfig.BASE_URL
            .replace("http://", "")
            .replace("https://", "")
            .removeSuffix("/")
        
        val protocol = if (BuildKonfig.BASE_URL.startsWith("https")) "wss" else "ws"

        return client.webSocketSession {
            url("$protocol://$host/game/$sessionId")
            header("Authorization", "Bearer ${sessionManager.getAccessToken()}")
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
