package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.ServerEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GameRepositoryImpl(private val client: GameClient) : GameRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 16)
    override val events: Flow<ServerEvent> = _events.asSharedFlow()

    // Outbound actions are buffered here and drained by the active connection.
    private val _outgoing = MutableSharedFlow<GameAction>(extraBufferCapacity = 16)

    private var connectionJob: Job? = null

    override suspend fun joinGame(playerId: String): JoinGameResponse = client.joinGame(playerId)

    override fun connect(playerId: String, gameId: String) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                client.connectToGame(playerId, gameId, _outgoing) { _events.emit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _events.emit(ConnectionFailed(e.message ?: "WebSocket connection failed"))
            }
        }
    }

    override fun sendAction(action: GameAction) {
        _outgoing.tryEmit(action)
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
    }
}
