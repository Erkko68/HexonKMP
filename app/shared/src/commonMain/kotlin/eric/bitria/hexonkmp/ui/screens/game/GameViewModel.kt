package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.action.BuyDevCard
import eric.bitria.hexonkmp.core.game.action.CancelTrade
import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.PlayKnight
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.engine.CatanEngine
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.CityUpgraded
import eric.bitria.hexonkmp.core.game.event.DevCardBought
import eric.bitria.hexonkmp.core.game.event.DevCardPlayed
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.GameEnded
import eric.bitria.hexonkmp.core.game.event.GameEvent
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
import eric.bitria.hexonkmp.core.game.event.LargestArmyChanged
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.DevCard
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
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.GameUpdate
import eric.bitria.hexonkmp.core.protocol.PlayerJoined
import eric.bitria.hexonkmp.core.protocol.PlayerLeft
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

    private var myPlayerId: PlayerId? = null

    // The same pure engine the server runs — used here only to query *legal*
    // placements so the UI can offer them (board config travels in GameState, so
    // a default-config instance is fine for read-only queries).
    private val engine: CatanEngine = CatanGameEngine()

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

    // Victory points: built pieces (settlement 1, city 2) + held Victory-Point dev
    // cards + Largest Army. For opponents we only know our own dev cards (theirs are
    // hidden), so an opponent's count here is a lower bound — exact for ourselves,
    // which is what the HUD badge shows.
    fun victoryPoints(s: GameUiState.InGame, player: PlayerId): Int {
        val st = s.state
        val fromBuildings = st.buildings.filter { it.owner == player }
            .sumOf { if (it.kind == Building.Kind.CITY) 2 else 1 }
        val fromVpCards = (st.devCards[player].orEmpty() + st.boughtThisTurn[player].orEmpty())
            .count { it == DevCard.VICTORY_POINT }
        val fromArmy = if (st.largestArmy == player) st.config.rules.largestArmyVp else 0
        return fromBuildings + fromVpCards + fromArmy
    }

    // Send: buy one development card (drawn server-side, hidden from opponents).
    fun buyDevCard() {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.isMyTurn) repository.sendAction(BuyDevCard)
    }

    // Dispatch a dev card play. The caller (confirm dialog) already verified the
    // card is in the player's hand and that canPlay conditions are met.
    fun playDevCard(card: DevCard) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        when (card) {
            DevCard.KNIGHT -> repository.sendAction(PlayKnight)
            DevCard.VICTORY_POINT -> Unit // can't be played
            DevCard.ROAD_BUILDING -> Unit // TODO
            DevCard.YEAR_OF_PLENTY -> Unit // TODO
            DevCard.MONOPOLY -> Unit // TODO
        }
    }

    // Send one atomic bank trade bundling all the chosen swaps.
    fun bankTrade(swaps: List<BankSwap>) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || swaps.isEmpty()) return
        repository.sendAction(BankTrade(swaps))
    }

    // --- Player-to-player trades ---

    // Tap-to-cycle the give side: +1 up to what you hold, wrapping to 0. A
    // resource already on the "want" side can't also be given.
    fun cycleGive(resource: Resource) {
        _state.update { s ->
            if (s !is GameUiState.InGame) s
            else {
                val hand = s.state.handOf(myPlayerId ?: return)
                val d = s.proposeDraft
                if (d.receive[resource] > 0) s
                else s.updated(proposeDraft = d.copy(give = d.give.cycle(resource, hand[resource])))
            }
        }
    }

    // Tap-to-cycle the want side (cap arbitrary). Can't request what you're giving.
    fun cycleReceive(resource: Resource) {
        _state.update { s ->
            if (s !is GameUiState.InGame) s
            else {
                val d = s.proposeDraft
                if (d.give[resource] > 0) s
                else s.updated(proposeDraft = d.copy(receive = d.receive.cycle(resource, MAX_RECEIVE)))
            }
        }
    }

    fun clearProposeDraft() {
        _state.update { s ->
            if (s is GameUiState.InGame) s.updated(proposeDraft = ProposeDraft()) else s
        }
    }

    // Send the drafted offer to all opponents (current player only), then reset
    // the draft. The engine re-validates; the sheet stays open to finalize a reply.
    fun submitProposal() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = s.proposeDraft
        if (!s.isMyTurn || draft.give.isEmpty || draft.receive.isEmpty) return
        repository.sendAction(ProposeTrade(draft.give, draft.receive))
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(proposeDraft = ProposeDraft()) else current
        }
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

    fun cycleDiscard(resource: Resource) {
        _state.update { s ->
            if (s !is GameUiState.InGame) s
            else {
                val hand = s.state.handOf(myPlayerId ?: return)
                s.updated(discardDraft = s.discardDraft.cycle(resource, hand[resource]))
            }
        }
    }

    fun clearDiscardDraft() {
        _state.update { s ->
            if (s is GameUiState.InGame) s.updated(discardDraft = ResourceCount()) else s
        }
    }

    // Submit the discard once it matches the owed count, then reset the draft.
    fun submitDiscard() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = s.discardDraft
        if (draft.total != discardOwed(s)) return
        repository.sendAction(DiscardResources(draft))
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(discardDraft = ResourceCount()) else current
        }
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
            else s.updated(buildMode = if (s.buildMode == mode) BuildMode.NONE else mode)
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
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(buildMode = BuildMode.NONE) else current
        }
    }

    fun pickEdge(edge: Edge) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.buildMode != BuildMode.ROAD) return
        repository.sendAction(PlaceRoad(edge))
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(buildMode = BuildMode.NONE) else current
        }
    }

    // Tap a tile during the Robber phase to relocate the robber there.
    fun pickHex(hex: Axial) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || s.state.phase !is GamePhase.Robber) return
        repository.sendAction(MoveRobber(hex))
    }

    // Compute all action affordances from a single pass over game state.
    // This is the single source of truth for what the UI may show or enable — the
    // Screen never reads GamePhase or state fields directly for these decisions.
    private fun computeBuildOptions(s: GameUiState.InGame): BuildOptions {
        val me = s.myPlayerId
        val phase = s.state.phase

        // Trade & badges: valid regardless of whose turn it is (opponents respond).
        val hasIncomingOffer = s.state.pendingTrades.any { it.proposer != me }
        val canTrade = phase is GamePhase.Play && (s.isMyTurn || hasIncomingOffer)
        val tradeBadge = if (s.isMyTurn) {
            s.state.pendingTrades.any { it.accepters.isNotEmpty() }
        } else {
            s.state.pendingTrades.any { it.proposer != me && me !in it.responses }
        }

        // End Turn: only visible in Play, only enabled on the local player's turn.
        val showEndTurn = phase is GamePhase.Play
        val canEndTurn = s.isMyTurn && showEndTurn

        if (!s.isMyTurn) return BuildOptions(
            canTrade = canTrade,
            tradeBadge = tradeBadge,
            showEndTurn = showEndTurn,
            canEndTurn = canEndTurn,
        )

        // Robber phase: local player must move the robber; no building or buying.
        if (phase is GamePhase.Robber) {
            val targets = s.state.board.tiles.map { it.hex }.filter { it != s.state.board.robber }
            return BuildOptions(
                robberTargets = targets,
                canTrade = canTrade,
                tradeBadge = tradeBadge,
                showEndTurn = showEndTurn,
                canEndTurn = canEndTurn,
            )
        }

        // My turn, non-Robber: compute full set of placement / purchase affordances.
        val settlements = engine.legalSettlements(s.state, me)
        val roads = engine.legalRoads(s.state, me)
        val cities = engine.legalCities(s.state, me)
        val canBuyDev = phase is GamePhase.Play &&
            s.state.devDeckSize > 0 &&
            engine.canAfford(s.state, me, Buildable.DEV_CARD)

        // Dev card playability: Play phase, one per turn, and the card must be in
        // the playable hand (not boughtThisTurn). Only implemented cards are included.
        val playableDevCards = if (phase is GamePhase.Play && !s.state.devCardPlayed) {
            s.state.devCards[me].orEmpty().filter { it == DevCard.KNIGHT }.toSet()
        } else emptySet()

        return BuildOptions(
            canSettlement = settlements.isNotEmpty(),
            canRoad = roads.isNotEmpty(),
            canCity = cities.isNotEmpty(),
            canBuyDevCard = canBuyDev,
            ghostSettlements = if (s.buildMode == BuildMode.SETTLEMENT) settlements.toList() else emptyList(),
            ghostRoads = if (s.buildMode == BuildMode.ROAD) roads.toList() else emptyList(),
            ghostCities = if (s.buildMode == BuildMode.CITY) cities.toList() else emptyList(),
            robberTargets = emptyList(),
            canTrade = canTrade,
            tradeBadge = tradeBadge,
            showEndTurn = showEndTurn,
            canEndTurn = canEndTurn,
            playableDevCards = playableDevCards,
        )
    }

    private fun GameUiState.InGame.updated(
        state: GameState = this.state,
        buildMode: BuildMode = this.buildMode,
        notice: String? = this.notice,
        proposeDraft: ProposeDraft = this.proposeDraft,
        discardDraft: ResourceCount = this.discardDraft,
    ): GameUiState.InGame {
        val next = copy(
            state = state,
            buildMode = buildMode,
            notice = notice,
            proposeDraft = proposeDraft,
            discardDraft = discardDraft
        )
        return next.copy(buildOptions = computeBuildOptions(next))
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

    private fun handleServerEvent(event: CatanServerEvent) {
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
                    GameUiState.InGame(gameId = s.gameId, state = event.state, myPlayerId = me).updated()
                } else s
            }
            // --- Presence: Catan-style, players coming and going don't end the
            // game for the others — just surface a notice. ---
            is PlayerJoined -> _state.update { s ->
                if (s is GameUiState.InGame) s.updated(notice = "Player ${event.playerId} joined") else s
            }
            is PlayerLeft -> _state.update { s ->
                if (s is GameUiState.InGame) s.updated(notice = "Player ${event.playerId} left") else s
            }
            // --- Game updates: apply the domain event to the local state copy. ---
            is GameUpdate -> _state.update { s ->
                if (s is GameUiState.InGame) s.updated(state = applyEvent(s, event)) else s
            }
            // --- Action feedback (only the acting player receives this) ---
            is ActionRejected -> _state.update { s ->
                if (s is GameUiState.InGame) s.updated(notice = event.reason) else s
            }
            // --- Client-local ---
            is ConnectionFailed -> {
                repository.disconnect()
                _state.value = GameUiState.Error(event.reason)
            }
        }
    }

    private fun applyEvent(s: GameUiState.InGame, update: GameUpdate<GameEvent>) =
        when (val e = update.event) {
            is TurnChanged -> {
                // Mirror the engine's beginTurn: our own bought-this-turn dev cards
                // mature into our playable hand, and the one-per-turn flag resets.
                val st = s.state
                val me = myPlayerId
                val matured = if (e.currentPlayer == me) st.boughtThisTurn[me].orEmpty() else emptyList()
                st.copy(
                    currentPlayerIndex = st.players.indexOf(e.currentPlayer),
                    turn = e.turn,
                    devCards = if (matured.isEmpty() || me == null) st.devCards
                    else st.devCards + (me to (st.devCards[me].orEmpty() + matured)),
                    boughtThisTurn = if (matured.isEmpty() || me == null) st.boughtThisTurn
                    else st.boughtThisTurn - me,
                    devCardPlayed = false,
                )
            }
            is DiceRolled -> s.state.copy(lastRoll = e.total)
            is ResourcesProduced -> s.state.applyResourceDeltas(e.gains)
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
            is BankTraded -> s.state.applyResourceDeltas(mapOf(e.player to (e.received - e.given)))
            // --- Player-to-player trades (mirror the server's pending-offer state) ---
            is TradeProposed -> s.state.copy(pendingTrades = s.state.pendingTrades + e.offer)
            is TradeResponded -> s.state.copy(
                pendingTrades = s.state.pendingTrades.map {
                    if (it.id == e.offerId) it.copy(responses = it.responses + (e.player to e.accepted)) else it
                },
            )
            is TradeFinalized -> s.state.applyResourceDeltas(
                // Proposer -give +receive; partner the reverse.
                mapOf(e.proposer to (e.receive - e.give), e.partner to (e.give - e.receive)),
            ).copy(pendingTrades = emptyList()) // finalizing clears all offers
            is TradeOffersCleared -> s.state.copy(pendingTrades = emptyList())
            is TradeCancelled -> s.state.copy(
                pendingTrades = s.state.pendingTrades.filterNot { it.id == e.offerId },
            )
            is ResourcesDiscarded -> s.state.applyResourceDeltas(mapOf(e.player to (ResourceCount() - e.cards)))
            is GameEnded -> s.state.copy(phase = GamePhase.Finished(e.winner))
            is RobberMoved -> s.state.copy(board = s.state.board.copy(robber = e.hex))
            // The stolen card's type is known only to thief/victim; for everyone else
            // [resource] is null and we move counts only (no exact hand to update).
            is ResourceStolen -> e.resource?.let { res ->
                s.state.applyResourceDeltas(
                    mapOf(e.by to ResourceCount.of(res to 1), e.from to (ResourceCount() - ResourceCount.of(res to 1))),
                )
            } ?: s.state.adjustCounts(mapOf(e.by to 1, e.from to -1))
            // --- Development cards ---
            is DevCardBought -> {
                // Our own buy reveals the card (-> our boughtThisTurn); an opponent's
                // only bumps their public count. The deck size is public either way,
                // and the cost is spent via the shared spend() helper.
                val st = s.state
                val mine = e.player == myPlayerId && e.card != null
                st.copy(
                    boughtThisTurn = if (!mine) st.boughtThisTurn
                    else st.boughtThisTurn + (e.player to (st.boughtThisTurn[e.player].orEmpty() + e.card!!)),
                    devCardCounts = st.devCardCounts + (e.player to ((st.devCardCounts[e.player] ?: 0) + 1)),
                    devDeckSize = e.deckSize,
                ).spend(e.player, Buildable.DEV_CARD)
            }
            is DevCardPlayed -> {
                // Public. Drop the card from our playable hand (if ours), bump knights,
                // lower the player's public count, and mark a dev card played this turn.
                val st = s.state
                val newDev = if (e.player == myPlayerId)
                    st.devCards + (e.player to st.devCards[e.player].orEmpty().toMutableList()
                        .apply { remove(e.card) }) else st.devCards
                st.copy(
                    devCards = newDev,
                    knightsPlayed = if (e.card == DevCard.KNIGHT)
                        st.knightsPlayed + (e.player to ((st.knightsPlayed[e.player] ?: 0) + 1))
                    else st.knightsPlayed,
                    devCardCounts = st.devCardCounts + (e.player to ((st.devCardCounts[e.player] ?: 0) - 1)),
                    devCardPlayed = true,
                )
            }
            is LargestArmyChanged -> s.state.copy(largestArmy = e.holder)
        }

    // Deducts a buildable's cost from the owner (the Play-phase build price).
    private fun GameState.spend(player: PlayerId, buildable: Buildable): GameState =
        applyResourceDeltas(mapOf(player to (ResourceCount() - config.rules.cost(buildable))))

    private val Building.Kind.buildable: Buildable
        get() = when (this) {
            Building.Kind.SETTLEMENT -> Buildable.SETTLEMENT
            Building.Kind.CITY -> Buildable.CITY
        }

    // Applies per-player resource deltas to the local state. Hidden information:
    // we only ever keep OUR OWN exact hand; for everyone we keep the public
    // resourceCounts in sync. Opponents' exact hands are never reconstructed (the
    // server doesn't send them), so the UI shows their count only.
    private fun GameState.applyResourceDeltas(deltas: Map<PlayerId, ResourceCount>): GameState {
        val me = myPlayerId
        val newHands = if (me != null && deltas.containsKey(me))
            hands + (me to (handOf(me) + deltas.getValue(me))) else hands
        return copy(hands = newHands).adjustCounts(deltas.mapValues { it.value.total })
    }

    // Adjusts only the public resource-card counts (used for hidden steals, where
    // a card changes hands but third parties don't learn its type).
    private fun GameState.adjustCounts(deltas: Map<PlayerId, Int>): GameState {
        val counts = resourceCounts.toMutableMap()
        for ((player, d) in deltas) counts[player] = (counts[player] ?: 0) + d
        return copy(resourceCounts = counts)
    }
}
