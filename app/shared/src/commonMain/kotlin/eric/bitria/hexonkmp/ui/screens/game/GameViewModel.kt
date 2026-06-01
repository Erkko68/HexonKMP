package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.engine.GameEngine
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.PhaseChanged
import eric.bitria.hexonkmp.core.game.event.ResourcesProduced
import eric.bitria.hexonkmp.core.game.event.RoadPlaced
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.protocol.ActionRejected
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.GameUpdate
import eric.bitria.hexonkmp.core.protocol.PlayerJoined
import eric.bitria.hexonkmp.core.protocol.PlayerLeft
import eric.bitria.hexonkmp.core.protocol.ServerEvent
import eric.bitria.hexonkmp.core.protocol.WaitingForPlayers
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val prefs: DevicePreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var myPlayerId: PlayerId? = null

    // The same pure engine the server runs — used here only to query *legal*
    // placements so the UI can offer them (board config travels in GameState, so
    // a default-config instance is fine for read-only queries).
    private val engine: GameEngine = CatanGameEngine()

    init {
        viewModelScope.launch {
            repository.events.collect { handleServerEvent(it) }
        }
    }

    fun joinGame() {
        if (_state.value != GameUiState.Idle) return
        viewModelScope.launch {
            _state.value = GameUiState.Connecting
            runCatching {
                val playerId = prefs.getOrCreatePlayerId()
                playerId to repository.joinGame(playerId)
            }
                .onSuccess { (playerId, response) ->
                    myPlayerId = PlayerId(playerId)
                    _state.value = GameUiState.Waiting(response.gameId)
                    repository.connect(playerId, response.gameId)
                }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Connection failed") }
        }
    }

    fun endTurn() {
        val s = _state.value
        if (s is GameUiState.InGame && s.isMyTurn) repository.sendAction(EndTurn)
    }

    // Build actions place at a RANDOM valid spot for now (board tap-to-place
    // comes later). During setup the engine offers the legal-move set directly;
    // during play any empty/valid spot works and the engine checks cost.
    fun buildSettlement() {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        val target = when (s.state.phase) {
            is GamePhase.Setup -> engine.legalSettlements(s.state, s.myPlayerId).randomOrNull()
            GamePhase.Play -> randomEmptyVertex(s)
        } ?: return
        repository.sendAction(PlaceSettlement(target))
    }

    fun buildRoad() {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        val target = when (s.state.phase) {
            is GamePhase.Setup -> engine.legalRoads(s.state, s.myPlayerId).randomOrNull()
            GamePhase.Play -> randomEmptyEdge(s)
        } ?: return
        repository.sendAction(PlaceRoad(target))
    }

    // Whether the given buildable's button should be enabled for me right now.
    fun canBuild(s: GameUiState.InGame, buildable: Buildable): Boolean {
        if (!s.isMyTurn) return false
        return when (s.state.phase) {
            // Setup: a placement is available iff the engine offers legal spots
            // for the piece currently awaited.
            is GamePhase.Setup -> when (buildable) {
                Buildable.SETTLEMENT -> engine.legalSettlements(s.state, s.myPlayerId).isNotEmpty()
                Buildable.ROAD -> engine.legalRoads(s.state, s.myPlayerId).isNotEmpty()
                else -> false
            }
            // Play: gated purely by affordability (random placement always finds a spot).
            GamePhase.Play -> engine.canAfford(s.state, s.myPlayerId, buildable)
        }
    }

    private fun randomEmptyVertex(s: GameUiState.InGame): Vertex? =
        s.state.board.vertices().filter { s.state.buildingAt(it) == null }.randomOrNull()

    private fun randomEmptyEdge(s: GameUiState.InGame): Edge? =
        s.state.board.edges().filter { s.state.roadAt(it) == null }.randomOrNull()

    fun retryJoinGame() {
        _state.value = GameUiState.Idle
        joinGame()
    }

    fun leaveGame() {
        repository.disconnect()
        _state.value = GameUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    private fun handleServerEvent(event: ServerEvent) {
        when (event) {
            // --- Lobby phase ---
            is WaitingForPlayers -> _state.update { s ->
                if (s is GameUiState.Waiting) s.copy(connected = event.connected, needed = event.needed)
                else s
            }
            is GameStarted -> _state.update { s ->
                // Fired when the room fills, or on reconnect into a running game.
                val me = myPlayerId
                if (s is GameUiState.Waiting && me != null) {
                    GameUiState.InGame(gameId = s.gameId, state = event.state, myPlayerId = me)
                } else s
            }
            // --- Presence: Catan-style, players coming and going don't end the
            // game for the others — just surface a notice. ---
            is PlayerJoined -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = "Player ${event.playerId} joined") else s
            }
            is PlayerLeft -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = "Player ${event.playerId} left") else s
            }
            // --- Game updates: apply the domain event to the local state copy. ---
            is GameUpdate -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(state = applyEvent(s, event)) else s
            }
            // --- Action feedback (only the acting player receives this) ---
            is ActionRejected -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = event.reason) else s
            }
            // --- Client-local ---
            is ConnectionFailed -> {
                repository.disconnect()
                _state.value = GameUiState.Error(event.reason)
            }
        }
    }

    private fun applyEvent(s: GameUiState.InGame, update: GameUpdate) =
        when (val e = update.event) {
            is TurnChanged -> s.state.copy(
                currentPlayerIndex = s.state.players.indexOf(e.currentPlayer),
                turn = e.turn,
            )
            is DiceRolled -> s.state.copy(lastRoll = e.total)
            is ResourcesProduced -> s.state.copy(hands = s.state.hands.merge(e.gains))
            is PhaseChanged -> s.state.copy(phase = e.phase)
            is BuildingPlaced -> s.state.copy(buildings = s.state.buildings + e.building)
            is RoadPlaced -> s.state.copy(roads = s.state.roads + e.road)
        }

    // Adds each player's gains onto their current hand.
    private fun Map<PlayerId, ResourceCount>.merge(
        gains: Map<PlayerId, ResourceCount>,
    ): Map<PlayerId, ResourceCount> {
        val result = toMutableMap()
        for ((player, gain) in gains) {
            result[player] = (result[player] ?: ResourceCount()) + gain
        }
        return result
    }
}
