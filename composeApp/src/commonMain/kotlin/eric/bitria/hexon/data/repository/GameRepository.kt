package eric.bitria.hexon.data.repository

import eric.bitria.hexon.data.remote.GameSocketClient
import eric.bitria.hexon.ws.GameMessage
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface GameRepository {
    // 1. Data Stream: A flow of incoming messages
    val incomingMessages: Flow<GameMessage>

    // 2. Connection Management
    suspend fun connect(sessionId: String)
    suspend fun disconnect()

    // 3. Actions
    suspend fun sendMessage(message: GameMessage)
}

class GameRepositoryImpl(
    private val client: GameSocketClient
) : GameRepository {

    // Internal State
    private var session: DefaultWebSocketSession? = null
    private val sessionMutex = Mutex()

    // Scope for background listening.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Flow for messages with initial buffering.
    private val _incomingMessages = MutableSharedFlow<GameMessage>(replay = 8)
    override val incomingMessages: Flow<GameMessage> = _incomingMessages.asSharedFlow()

    override suspend fun connect(sessionId: String) {
        sessionMutex.withLock {
            // 1. Idempotency Check: If already connected, do nothing.
            if (session?.isActive == true) return

            try {
                // 2. Connect
                val newSession = client.connect(sessionId)
                session = newSession

                // 3. Start Listening (Survives ViewModel destruction)
                repositoryScope.launch {
                    try {
                        client.observeMessages(newSession).collect { msg ->
                            _incomingMessages.emit(msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        // Optional: Handle auto-disconnect logic here
                    }
                }
            } catch (e: Exception) {
                session = null
                throw e
            }
        }
    }

    override suspend fun sendMessage(message: GameMessage) {
        val currentSession = session
        if (currentSession != null && currentSession.isActive) {
            client.sendMessage(currentSession, message)
        }
    }

    override suspend fun disconnect() {
        sessionMutex.withLock {
            try {
                session?.close()
            } catch (e: Exception) {
                // Ignore close errors
            } finally {
                session = null
            }
        }
    }
}