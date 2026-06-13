package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.CreateLobbyResponse
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.JoinLobbyResponse
import eric.bitria.hexonkmp.core.protocol.PartyRules
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import eric.bitria.hexonkmp.core.protocol.TurnTimer
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

    private val _events = MutableSharedFlow<CatanServerEvent>(extraBufferCapacity = 16)
    override val events: Flow<CatanServerEvent> = _events.asSharedFlow()

    // Outbound actions are buffered here and drained by the active connection.
    private val _outgoing = MutableSharedFlow<GameAction>(extraBufferCapacity = 16)

    private var connectionJob: Job? = null

    override var currentPlayerId: String? = null
        private set
    override var currentGameId: String? = null
        private set
    override var startedGame: GameState? = null
        private set
    override var startedNames: Map<String, String> = emptyMap()
        private set
    override var startedTurnRemainingSeconds: Int? = null
        private set

    override suspend fun register(name: String, token: String?): RegisterResponse =
        client.register(name, token)

    override suspend fun joinGame(): JoinGameResponse = client.joinGame()

    override suspend fun createLobby(): CreateLobbyResponse = client.createLobby()

    override suspend fun joinLobby(code: String): JoinLobbyResponse = client.joinLobby(code)

    override suspend fun startLobby(gameId: String, rules: PartyRules) =
        client.startLobby(gameId, rules)

    override fun connect(token: String, playerId: String, name: String, gameId: String) {
        connectionJob?.cancel()
        currentPlayerId = playerId
        currentGameId = gameId
        connectionJob = scope.launch {
            try {
                client.connectToGame(token, name, gameId, _outgoing) { event ->
                    // Cache the start snapshot for the game screen's handoff (it's
                    // created only after the lobby reacts to this same event).
                    if (event is GameStarted<*>) {
                        @Suppress("UNCHECKED_CAST")
                        startedGame = (event as GameStarted<GameState>).state
                        startedNames = event.playerNames
                    }
                    // Cache the latest turn clock for the handoff (same race as above).
                    if (event is TurnTimer) startedTurnRemainingSeconds = event.remainingSeconds
                    _events.emit(event)
                }
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
        startedGame = null
        startedNames = emptyMap()
        startedTurnRemainingSeconds = null
        currentPlayerId = null
        currentGameId = null
    }
}
