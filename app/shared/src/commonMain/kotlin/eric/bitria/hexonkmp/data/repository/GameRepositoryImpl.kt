package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.core.dto.JoinGameResponse
import eric.bitria.hexonkmp.core.ws.ServerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GameRepositoryImpl(private val client: GameClient) : GameRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 16)
    override val events: Flow<ServerEvent> = _events.asSharedFlow()

    private var connectionJob: Job? = null

    override suspend fun joinGame(playerId: String): JoinGameResponse = client.joinGame(playerId)

    override fun connect(playerId: String, gameId: String) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            client.connectToGame(playerId, gameId) { _events.emit(it) }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
    }
}
