package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.action.CancelTrade
import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.engine.GameEngine
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.CityUpgraded
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.PhaseChanged
import eric.bitria.hexonkmp.core.game.event.ResourcesProduced
import eric.bitria.hexonkmp.core.game.event.RoadPlaced
import eric.bitria.hexonkmp.core.game.event.RobberMoved
import eric.bitria.hexonkmp.core.game.event.ResourceStolen
import eric.bitria.hexonkmp.core.game.event.ResourcesDiscarded
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.event.TradeCancelled
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
import eric.bitria.hexonkmp.core.game.model.board.Axial
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Upper bound for how many of a resource you can request in one proposed trade.
private const val MAX_RECEIVE = 9

class GameViewModel(
    private val repository: GameRepository,
    private val prefs: DevicePreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    // Derived placement options (which build cards to enable, ghost markers to
    // draw). Computed once per state change — including buildMode toggles, which
    // are part of the state — off the UI/composition, so the legal-move board
    // scans never run during recomposition.
    val buildOptions: StateFlow<BuildOptions> =
        _state.map { (it as? GameUiState.InGame)?.let(::computeBuildOptions) ?: BuildOptions.NONE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BuildOptions.NONE)

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

    // Victory points from built pieces (settlement 1, city 2). Dev cards /
    // longest road / largest army aren't counted yet.
    fun victoryPoints(s: GameUiState.InGame, player: PlayerId): Int =
        s.state.buildings.filter { it.owner == player }
            .sumOf { if (it.kind == Building.Kind.CITY) 2 else 1 }

    // Send one atomic bank trade bundling all the chosen swaps.
    fun bankTrade(swaps: List<BankSwap>) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || swaps.isEmpty()) return
        repository.sendAction(BankTrade(swaps))
    }

    // --- Player-to-player trades ---

    data class ProposeDraft(
        val give: ResourceCount = ResourceCount(),
        val receive: ResourceCount = ResourceCount(),
    )

    private val _proposeDraft = MutableStateFlow(ProposeDraft())
    val proposeDraft: StateFlow<ProposeDraft> = _proposeDraft.asStateFlow()

    // Tap-to-cycle the give side: +1 up to what you hold, wrapping to 0. A
    // resource already on the "want" side can't also be given.
    fun cycleGive(resource: Resource) {
        val hand = (_state.value as? GameUiState.InGame)?.state?.handOf(myPlayerId ?: return) ?: return
        _proposeDraft.update { d ->
            if (d.receive[resource] > 0) d else d.copy(give = d.give.cycle(resource, hand[resource]))
        }
    }

    // Tap-to-cycle the want side (cap arbitrary). Can't request what you're giving.
    fun cycleReceive(resource: Resource) {
        _proposeDraft.update { d ->
            if (d.give[resource] > 0) d else d.copy(receive = d.receive.cycle(resource, MAX_RECEIVE))
        }
    }

    fun clearProposeDraft() { _proposeDraft.value = ProposeDraft() }

    // Send the drafted offer to all opponents (current player only), then reset
    // the draft. The engine re-validates; the sheet stays open to finalize a reply.
    fun submitProposal() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = _proposeDraft.value
        if (!s.isMyTurn || draft.give.isEmpty || draft.receive.isEmpty) return
        repository.sendAction(ProposeTrade(draft.give, draft.receive))
        _proposeDraft.value = ProposeDraft()
    }

    // +1 a single resource's count up to [max], wrapping back to 0.
    private fun ResourceCount.cycle(resource: Resource, max: Int): ResourceCount {
        val next = if (this[resource] + 1 > max) 0 else this[resource] + 1
        return ResourceCount((amounts + (resource to next)).filterValues { it != 0 })
    }

    // --- Discard (rolled a 7, over the hand limit) ---

    // How many cards the local player must discard right now (0 if none).
    fun discardOwed(s: GameUiState.InGame): Int =
        (s.state.phase as? GamePhase.Discard)?.pending?.get(s.myPlayerId) ?: 0

    // The in-progress discard selection, its own flow like the propose draft.
    private val _discardDraft = MutableStateFlow(ResourceCount())
    val discardDraft: StateFlow<ResourceCount> = _discardDraft.asStateFlow()

    fun cycleDiscard(resource: Resource) {
        val hand = (_state.value as? GameUiState.InGame)?.state?.handOf(myPlayerId ?: return) ?: return
        _discardDraft.update { it.cycle(resource, hand[resource]) }
    }

    fun clearDiscardDraft() { _discardDraft.value = ResourceCount() }

    // Submit the discard once it matches the owed count, then reset the draft.
    fun submitDiscard() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = _discardDraft.value
        if (draft.total != discardOwed(s)) return
        repository.sendAction(DiscardResources(draft))
        _discardDraft.value = ResourceCount()
    }

    // Accept or decline an opponent's pending offer (allowed off-turn).
    fun respondTrade(offerId: Int, accept: Boolean) {
        if (_state.value !is GameUiState.InGame) return
        repository.sendAction(RespondTrade(offerId, accept))
    }

    // Finalize a pending offer with a player who accepted (proposer only).
    fun finalizeTrade(offerId: Int, partner: PlayerId) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        repository.sendAction(FinalizeTrade(offerId, partner))
    }

    // Withdraw one of your own pending offers (proposer only).
    fun cancelTrade(offerId: Int) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        repository.sendAction(CancelTrade(offerId))
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
    // A vertex tap means a settlement or a city depending on the armed mode.
    fun pickVertex(vertex: Vertex) {
        val s = _state.value as? GameUiState.InGame ?: return
        when (s.buildMode) {
            BuildMode.SETTLEMENT -> repository.sendAction(PlaceSettlement(vertex))
            BuildMode.CITY -> repository.sendAction(UpgradeCity(vertex))
            else -> return
        }
        _state.update { (it as? GameUiState.InGame)?.copy(buildMode = BuildMode.NONE) ?: it }
    }

    fun pickEdge(edge: Edge) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.buildMode != BuildMode.ROAD) return
        repository.sendAction(PlaceRoad(edge))
        _state.update { (it as? GameUiState.InGame)?.copy(buildMode = BuildMode.NONE) ?: it }
    }

    // Tap a tile during the Robber phase to relocate the robber there.
    fun pickHex(hex: Axial) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || s.state.phase !is GamePhase.Robber) return
        repository.sendAction(MoveRobber(hex))
    }

    // Everything the HUD needs about placement, computed from a SINGLE pass over
    // the board's legal moves: which build cards to enable, and the ghost markers
    // to draw for the armed mode. Surfaced through the [buildOptions] flow so the
    // whole-board legal-move scans run once per state change, off composition.
    data class BuildOptions(
        val canSettlement: Boolean,
        val canRoad: Boolean,
        val canCity: Boolean,
        val ghostSettlements: List<Vertex>,
        val ghostRoads: List<Edge>,
        val ghostCities: List<Vertex>,
        // Tiles the current player may move the robber to (Robber phase only).
        val robberTargets: List<Axial>,
    ) {
        companion object {
            val NONE = BuildOptions(false, false, false, emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    private fun computeBuildOptions(s: GameUiState.InGame): BuildOptions {
        if (!s.isMyTurn) return BuildOptions.NONE
        if (s.state.phase is GamePhase.Robber) {
            val targets = s.state.board.tiles.map { it.hex }.filter { it != s.state.board.robber }
            return BuildOptions.NONE.copy(robberTargets = targets)
        }
        val settlements = engine.legalSettlements(s.state, s.myPlayerId)
        val roads = engine.legalRoads(s.state, s.myPlayerId)
        val cities = engine.legalCities(s.state, s.myPlayerId)
        return BuildOptions(
            canSettlement = settlements.isNotEmpty(),
            canRoad = roads.isNotEmpty(),
            canCity = cities.isNotEmpty(),
            ghostSettlements = if (s.buildMode == BuildMode.SETTLEMENT) settlements.toList() else emptyList(),
            ghostRoads = if (s.buildMode == BuildMode.ROAD) roads.toList() else emptyList(),
            ghostCities = if (s.buildMode == BuildMode.CITY) cities.toList() else emptyList(),
            robberTargets = emptyList(),
        )
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
            is CityUpgraded -> s.state.copy(
                buildings = s.state.buildings.map { if (it.vertex == e.building.vertex) e.building else it },
            ).spend(e.building.owner, Buildable.CITY)
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
            is TradeCancelled -> s.state.copy(
                pendingTrades = s.state.pendingTrades.filterNot { it.id == e.offerId },
            )
            is ResourcesDiscarded -> s.state.copy(
                hands = s.state.hands.merge(mapOf(e.player to (ResourceCount() - e.cards))),
            )
            is RobberMoved -> s.state.copy(board = s.state.board.copy(robber = e.hex))
            is ResourceStolen -> s.state.copy(
                hands = s.state.hands
                    .merge(mapOf(e.by to ResourceCount.of(e.resource to 1)))
                    .merge(mapOf(e.from to (ResourceCount() - ResourceCount.of(e.resource to 1)))),
            )
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
