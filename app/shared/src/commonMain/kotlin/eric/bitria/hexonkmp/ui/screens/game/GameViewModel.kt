package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
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
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.event.TradeFinalized
import eric.bitria.hexonkmp.core.game.event.TradeOffersCleared
import eric.bitria.hexonkmp.core.game.event.TradeProposed
import eric.bitria.hexonkmp.core.game.event.TradeResponded
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState
// BuildMode is in this package (GameUiState.kt)
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
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

    // --- Bank trade ---

    // The bank's exchange ratio (e.g. 4:1) for the current game.
    fun bankTradeRatio(s: GameUiState.InGame): Int = s.state.config.rules.bankTradeRatio

    // Resources the player holds enough of to trade to the bank (>= ratio).
    fun tradableResources(s: GameUiState.InGame): List<Resource> {
        if (!s.isMyTurn || s.state.phase !is GamePhase.Play) return emptyList()
        val ratio = bankTradeRatio(s)
        val hand = s.state.handOf(s.myPlayerId)
        return Resource.entries.filter { hand[it] >= ratio }
    }

    // Send one atomic bank trade bundling all the chosen swaps.
    fun bankTrade(swaps: List<BankSwap>) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || swaps.isEmpty()) return
        repository.sendAction(BankTrade(swaps))
    }

    // Build cards toggle a "build mode": entering it shows the legal spots as
    // ghost markers on the board; the player then taps one to place. Tapping the
    // active card again cancels.
    fun toggleBuildMode(mode: BuildMode) {
        _state.update { s ->
            if (s !is GameUiState.InGame || !s.isMyTurn) s
            else s.copy(buildMode = if (s.buildMode == mode) BuildMode.NONE else mode)
        }
    }

    // Tap handlers from the board: place at the picked location, then leave build mode.
    fun pickVertex(vertex: Vertex) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.buildMode != BuildMode.SETTLEMENT) return
        repository.sendAction(PlaceSettlement(vertex))
        _state.update { (it as? GameUiState.InGame)?.copy(buildMode = BuildMode.NONE) ?: it }
    }

    fun pickEdge(edge: Edge) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.buildMode != BuildMode.ROAD) return
        repository.sendAction(PlaceRoad(edge))
        _state.update { (it as? GameUiState.InGame)?.copy(buildMode = BuildMode.NONE) ?: it }
    }

    // Legal ghost spots to render, only when the matching build mode is active.
    fun ghostSettlements(s: GameUiState.InGame): List<Vertex> =
        if (s.buildMode == BuildMode.SETTLEMENT) engine.legalSettlements(s.state, s.myPlayerId).toList()
        else emptyList()

    fun ghostRoads(s: GameUiState.InGame): List<Edge> =
        if (s.buildMode == BuildMode.ROAD) engine.legalRoads(s.state, s.myPlayerId).toList()
        else emptyList()

    // Whether the given buildable's card should be enabled — i.e. at least one
    // legal placement exists right now (setup awaits it / play affords it).
    fun canBuild(s: GameUiState.InGame, buildable: Buildable): Boolean {
        if (!s.isMyTurn) return false
        return when (buildable) {
            Buildable.SETTLEMENT -> engine.legalSettlements(s.state, s.myPlayerId).isNotEmpty()
            Buildable.ROAD -> engine.legalRoads(s.state, s.myPlayerId).isNotEmpty()
            else -> false
        }
    }

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
                // In Play, building costs resources (Setup placement is free) — mirror
                // the engine's spend so the local hand matches the server.
                .let { if (it.phase is GamePhase.Play) it.spend(e.building.owner, e.building.kind.buildable) else it }
            is RoadPlaced -> s.state.copy(roads = s.state.roads + e.road)
                .let { if (it.phase is GamePhase.Play) it.spend(e.road.owner, Buildable.ROAD) else it }
            is BankTraded -> s.state.copy(
                hands = s.state.hands.merge(mapOf(e.player to e.received))
                    .merge(mapOf(e.player to (ResourceCount() - e.given))),
            )
            // --- Player-to-player trades (mirror the server's pending-offer state) ---
            is TradeProposed -> s.state.copy(pendingTrades = s.state.pendingTrades + e.offer)
            is TradeResponded -> s.state.copy(
                pendingTrades = s.state.pendingTrades.map {
                    if (it.id == e.offerId) it.copy(responses = it.responses + (e.player to e.accepted)) else it
                },
            )
            is TradeFinalized -> s.state.copy(
                // Proposer -give +receive; partner the reverse. Finalizing clears all offers.
                hands = s.state.hands
                    .merge(mapOf(e.proposer to e.receive)).merge(mapOf(e.proposer to (ResourceCount() - e.give)))
                    .merge(mapOf(e.partner to e.give)).merge(mapOf(e.partner to (ResourceCount() - e.receive))),
                pendingTrades = emptyList(),
            )
            is TradeOffersCleared -> s.state.copy(pendingTrades = emptyList())
        }

    // Deducts a buildable's cost from the owner's hand (the Play-phase build price).
    private fun GameState.spend(player: PlayerId, buildable: Buildable): GameState {
        val cost = config.rules.cost(buildable)
        return copy(hands = hands.merge(mapOf(player to (ResourceCount() - cost))))
    }

    private val Building.Kind.buildable: Buildable
        get() = when (this) {
            Building.Kind.SETTLEMENT -> Buildable.SETTLEMENT
            Building.Kind.CITY -> Buildable.CITY
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
