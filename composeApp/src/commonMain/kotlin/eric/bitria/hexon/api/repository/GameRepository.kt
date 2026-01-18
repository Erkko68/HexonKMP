package eric.bitria.hexon.api.repository

import eric.bitria.hexon.api.client.GameSocketClient
import eric.bitria.hexon.ws.GameMessage
import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun connect(sessionId: String): DefaultWebSocketSession
    fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage>
    suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage)
}

class GameRepositoryImpl(
    private val client: GameSocketClient
) : GameRepository {

    override suspend fun connect(sessionId: String): DefaultWebSocketSession {
        return client.connect(sessionId)
    }

    override fun observeMessages(session: DefaultWebSocketSession): Flow<GameMessage> {
        return client.observeMessages(session)
    }

    override suspend fun sendMessage(session: DefaultWebSocketSession, message: GameMessage) {
        client.sendMessage(session, message)
    }
}
