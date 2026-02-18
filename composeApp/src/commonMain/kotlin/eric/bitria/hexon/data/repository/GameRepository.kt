package eric.bitria.hexon.data.repository

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "GameRepository"

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
    private val _incomingMessages = MutableSharedFlow<GameMessage>(replay = 4)
    override val incomingMessages: Flow<GameMessage> = _incomingMessages.asSharedFlow()

    override suspend fun connect(sessionId: String) {
        Logger.d(TAG) { "connect() called with sessionId: $sessionId" }
        sessionMutex.withLock {
            Logger.d(TAG) { "Acquired session lock" }

            // 1. Idempotency Check: If already connected, do nothing.
            if (session?.isActive == true) {
                Logger.d(TAG) { "Already connected, skipping connection" }
                return
            }

            try {
                // 2. Connect
                Logger.d(TAG) { "Calling client.connect()..." }
                val newSession = client.connect(sessionId)
                session = newSession
                Logger.d(TAG) { "Session stored, starting message listener..." }

                // 3. Start Listening (Survives ViewModel destruction)
                repositoryScope.launch {
                    Logger.d(TAG) { "Message listener coroutine started" }
                    try {
                        var messageCount = 0
                        client.observeMessages(newSession).collect { msg ->
                            messageCount++
                            Logger.d(TAG) { "Received message #$messageCount: ${msg::class.simpleName}" }
                            _incomingMessages.emit(msg)
                            Logger.d(TAG) { "Message #$messageCount emitted to flow" }
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, e) { "Error in message listener: ${e.message}" }
                        e.printStackTrace()
                    } finally {
                        Logger.d(TAG) { "Message listener coroutine finished" }
                        // Optional: Handle auto-disconnect logic here
                    }
                }
                Logger.d(TAG) { "Connection setup complete" }
            } catch (e: Exception) {
                Logger.e(TAG, e) { "Failed to connect: ${e.message}" }
                session = null
                throw e
            }
        }
    }

    override suspend fun sendMessage(message: GameMessage) {
        Logger.d(TAG) { "sendMessage() called with message type: ${message::class.simpleName}" }
        val currentSession = session
        if (currentSession != null && currentSession.isActive) {
            Logger.d(TAG) { "Session is active, sending message..." }
            client.sendMessage(currentSession, message)
            Logger.d(TAG) { "Message sent successfully" }
        } else {
            Logger.w(TAG) { "Cannot send message: session is null or inactive" }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun disconnect() {
        Logger.d(TAG) { "disconnect() called" }
        sessionMutex.withLock {
            Logger.d(TAG) { "Acquired session lock for disconnect" }
            try {
                session?.close()
                Logger.d(TAG) { "Session closed successfully" }
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Error closing session: ${e.message}" }
                // Ignore close errors
            } finally {
                session = null
                Logger.d(TAG) { "Session set to null" }
            }
        }

        // Clear the replay buffer to prevent old messages from being replayed
        // to new subscribers (e.g., when a new GameViewModel is created after
        // a game ends)
        Logger.d(TAG) { "Resetting message buffer" }
        _incomingMessages.resetReplayCache()
        Logger.d(TAG) { "disconnect() completed" }
    }
}