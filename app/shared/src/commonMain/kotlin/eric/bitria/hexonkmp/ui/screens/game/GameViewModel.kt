package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import eric.bitria.hexonkmp.core.game.action.PlayMonopoly
import eric.bitria.hexonkmp.core.game.action.PlayRoadBuilding
import eric.bitria.hexonkmp.core.game.action.PlayYearOfPlenty
import eric.bitria.hexonkmp.core.game.action.StealFrom
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
import eric.bitria.hexonkmp.core.game.event.MonopolyUsed
import eric.bitria.hexonkmp.core.game.event.TradeCancelled
import eric.bitria.hexonkmp.core.game.event.TradeFinalized
import eric.bitria.hexonkmp.core.game.event.TradeOffersCleared
import eric.bitria.hexonkmp.core.game.event.TradeProposed
import eric.bitria.hexonkmp.core.game.event.TradeResponded
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.event.LargestArmyChanged
import eric.bitria.hexonkmp.core.game.event.LongestRoadChanged
import eric.bitria.hexonkmp.core.game.event.YearOfPlentyUsed
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
import eric.bitria.hexonkmp.core.protocol.LobbyRoster
import eric.bitria.hexonkmp.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Upper bound for how many of a resource you can request in one proposed trade.
private const val MAX_RECEIVE = 9

// How long a transient notice stays before auto-clearing.
private const val NOTICE_TIMEOUT_MS = 4000L

class GameViewModel(
    private val repository: GameRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var myPlayerId: PlayerId? = null

    // The same pure engine the server runs — used here only to query *legal*
    // placements so the UI can offer them (board config travels in GameState, so
    // a default-config instance is fine for read-only queries).
    private val engine: CatanEngine = CatanGameEngine()

    init {
        // Seed from the lobby->game handoff: the connection is already live and the
        // LobbyViewModel cached the start snapshot in the repository before navigating
        // here. We pick it up rather than racing to catch the GameStarted event.
        val started = repository.startedGame
        val me = repository.currentPlayerId
        if (started != null && me != null) {
            myPlayerId = PlayerId(me)
            _state.value = GameUiState.InGame(
                gameId = repository.currentGameId.orEmpty(),
                state = started,
                myPlayerId = PlayerId(me),
                playerNames = repository.startedNames.mapKeys { PlayerId(it.key) },
            ).updated()
        }
        viewModelScope.launch {
            repository.events.collect { handleServerEvent(it) }
        }
    }

    fun endTurn() {
        val s = _state.value
        if (s is GameUiState.InGame && s.isMyTurn) repository.sendAction(EndTurn)
    }

    // --- Bank trade ---

    // Send the current draft as a bank trade (give -> receive at the player's
    // ratios), then reset the draft. The engine re-validates authoritatively.
    fun submitBankTrade() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = s.tradeDraft
        if (!s.isMyTurn || !s.buildOptions.canBankTrade) return
        repository.sendAction(BankTrade(draft.give, draft.receive))
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(tradeDraft = TradeDraft()) else current
        }
    }

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
        val fromRoad = if (st.longestRoad == player) st.config.rules.longestRoadVp else 0
        return fromBuildings + fromVpCards + fromArmy + fromRoad
    }

    // Send: buy one development card (drawn server-side, hidden from opponents).
    fun buyDevCard() {
        val s = _state.value as? GameUiState.InGame ?: return
        if (s.isMyTurn) repository.sendAction(BuyDevCard)
    }

    // Dispatch a dev card play. ROAD_BUILDING sends immediately (engine manages
    // the RoadBuilding phase); YEAR_OF_PLENTY and MONOPOLY need a picker sheet
    // in the UI first — the layout calls playYearOfPlenty / playMonopoly directly.
    fun playDevCard(card: DevCard) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        when (card) {
            DevCard.KNIGHT -> repository.sendAction(PlayKnight)
            DevCard.ROAD_BUILDING -> repository.sendAction(PlayRoadBuilding)
            DevCard.VICTORY_POINT -> Unit // passive, never played
            DevCard.YEAR_OF_PLENTY -> Unit // handled by the YearOfPlentySheet via playYearOfPlenty()
            DevCard.MONOPOLY -> Unit // handled by the MonopolySheet via playMonopoly()
        }
    }

    fun playYearOfPlenty(resources: ResourceCount) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        repository.sendAction(PlayYearOfPlenty(resources))
    }

    fun playMonopoly(resource: Resource) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn) return
        repository.sendAction(PlayMonopoly(resource))
    }

    // --- Player-to-player trades ---

    // Tap-to-cycle the give side: +1 up to what you hold, wrapping to 0. A
    // resource already on the "want" side can't also be given.
    fun cycleGive(resource: Resource) {
        _state.update { s ->
            if (s !is GameUiState.InGame) s
            else {
                val hand = s.state.handOf(myPlayerId ?: return)
                val d = s.tradeDraft
                if (d.receive[resource] > 0) s
                else s.updated(tradeDraft = d.copy(give = d.give.cycle(resource, hand[resource])))
            }
        }
    }

    // Tap-to-cycle the want side (cap arbitrary). Can't request what you're giving.
    fun cycleReceive(resource: Resource) {
        _state.update { s ->
            if (s !is GameUiState.InGame) s
            else {
                val d = s.tradeDraft
                if (d.give[resource] > 0) s
                else s.updated(tradeDraft = d.copy(receive = d.receive.cycle(resource, MAX_RECEIVE)))
            }
        }
    }

    fun clearTradeDraft() {
        _state.update { s ->
            if (s is GameUiState.InGame) s.updated(tradeDraft = TradeDraft()) else s
        }
    }

    // Send the drafted offer to all opponents (current player only), then reset
    // the draft. The engine re-validates; the sheet stays open to finalize a reply.
    fun submitProposal() {
        val s = _state.value as? GameUiState.InGame ?: return
        val draft = s.tradeDraft
        if (!s.isMyTurn || draft.give.isEmpty || draft.receive.isEmpty) return
        repository.sendAction(ProposeTrade(draft.give, draft.receive))
        _state.update { current ->
            if (current is GameUiState.InGame) current.updated(tradeDraft = TradeDraft()) else current
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
        val isRoadBuilding = s.state.phase is GamePhase.RoadBuilding
        if (!isRoadBuilding && s.buildMode != BuildMode.ROAD) return
        repository.sendAction(PlaceRoad(edge))
        // During RoadBuilding the phase drives ghost roads — no buildMode to clear.
        if (!isRoadBuilding) {
            _state.update { current ->
                if (current is GameUiState.InGame) current.updated(buildMode = BuildMode.NONE) else current
            }
        }
    }

    // Tap a tile during the Robber phase to relocate the robber there.
    fun pickHex(hex: Axial) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || s.state.phase !is GamePhase.Robber) return
        repository.sendAction(MoveRobber(hex))
    }

    // Choose which adjacent opponent to steal from (ChooseStealTarget phase only).
    fun stealFrom(target: PlayerId) {
        val s = _state.value as? GameUiState.InGame ?: return
        if (!s.isMyTurn || s.state.phase !is GamePhase.ChooseStealTarget) return
        repository.sendAction(StealFrom(target))
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

        // ChooseStealTarget phase: local player must pick a victim; no other actions.
        if (phase is GamePhase.ChooseStealTarget) {
            return BuildOptions(
                canTrade = canTrade,
                tradeBadge = tradeBadge,
                showEndTurn = showEndTurn,
                canEndTurn = canEndTurn,
            )
        }

        // Road Building phase: show free road ghost markers; no other actions.
        if (phase is GamePhase.RoadBuilding) {
            val roads = engine.legalRoads(s.state, me)
            return BuildOptions(
                canRoad = roads.isNotEmpty(),
                ghostRoads = roads.toList(),
                showEndTurn = false,
                canEndTurn = false,
            )
        }

        // My turn, Play phase: compute full set of placement / purchase affordances.
        val settlements = engine.legalSettlements(s.state, me)
        val roads = engine.legalRoads(s.state, me)
        val cities = engine.legalCities(s.state, me)
        val canBuyDev = phase is GamePhase.Play &&
            s.state.devDeckSize > 0 &&
            engine.canAfford(s.state, me, Buildable.DEV_CARD)

        // All non-VP cards in the playable hand are actionable (one per turn).
        val playableDevCards = if (phase is GamePhase.Play && !s.state.devCardPlayed) {
            s.state.devCards[me].orEmpty().filter { it != DevCard.VICTORY_POINT }.toSet()
        } else emptySet()

        // Bank trading: the player's discounted ratios + whether the live draft is
        // a legal bank trade (shared engine predicate, single source of truth).
        val bankRates = engine.bankRates(s.state, me)
        val canBankTrade = engine.bankTradeRejection(
            s.state, me, s.tradeDraft.give, s.tradeDraft.receive,
        ) == null

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
            bankRates = bankRates,
            canBankTrade = canBankTrade,
            showEndTurn = showEndTurn,
            canEndTurn = canEndTurn,
            playableDevCards = playableDevCards,
        )
    }

    private fun GameUiState.InGame.updated(
        state: GameState = this.state,
        buildMode: BuildMode = this.buildMode,
        notice: String? = this.notice,
        tradeDraft: TradeDraft = this.tradeDraft,
        discardDraft: ResourceCount = this.discardDraft,
    ): GameUiState.InGame {
        val next = copy(
            state = state,
            buildMode = buildMode,
            notice = notice,
            tradeDraft = tradeDraft,
            discardDraft = discardDraft
        )
        return next.copy(buildOptions = computeBuildOptions(next))
    }

    // Tear down the connection. Navigation back to the lobby is the screen's job
    // (it calls this, then pops the back stack).
    fun leaveGame() {
        repository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    // Transient on-screen notices (a player joining/leaving, a rejected action) auto-
    // clear after a few seconds so they don't linger until the next game update.
    private var noticeJob: Job? = null
    private fun showNotice(text: String) {
        noticeJob?.cancel()
        _state.update { if (it is GameUiState.InGame) it.updated(notice = text) else it }
        noticeJob = viewModelScope.launch {
            delay(NOTICE_TIMEOUT_MS)
            // Only clear if it's still this notice (a newer one may have replaced it).
            _state.update { if (it is GameUiState.InGame && it.notice == text) it.updated(notice = null) else it }
        }
    }

    private fun displayNameOf(playerId: String): String =
        (_state.value as? GameUiState.InGame)?.displayName(PlayerId(playerId)) ?: playerId

    private fun handleServerEvent(event: CatanServerEvent) {
        when (event) {
            // Lobby-only events: handled by the LobbyViewModel, ignored here.
            is LobbyRoster -> Unit
            // GameStarted reaches us only on reconnect into a running game (the
            // initial start is consumed via the handoff seed in init). Rebuild state.
            is GameStarted -> _state.update {
                val me = myPlayerId ?: repository.currentPlayerId?.let { id -> PlayerId(id) }
                if (me != null) {
                    myPlayerId = me
                    GameUiState.InGame(
                        gameId = repository.currentGameId.orEmpty(),
                        state = event.state,
                        myPlayerId = me,
                        playerNames = event.playerNames.mapKeys { PlayerId(it.key) },
                    ).updated()
                } else it
            }
            // --- Presence: Catan-style, players coming and going don't end the
            // game for the others — just surface a notice. ---
            is PlayerJoined -> showNotice("${displayNameOf(event.playerId)} joined")
            is PlayerLeft -> showNotice("${displayNameOf(event.playerId)} left")
            // --- Game updates: apply the domain event to the local state copy. ---
            is GameUpdate -> _state.update { s ->
                if (s is GameUiState.InGame) s.updated(state = applyEvent(s, event)) else s
            }
            // --- Action feedback (only the acting player receives this) ---
            is ActionRejected -> showNotice(event.reason)
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
            is LongestRoadChanged -> s.state.copy(longestRoad = e.holder)
            is YearOfPlentyUsed -> s.state.applyResourceDeltas(mapOf(e.player to e.resources))
            is MonopolyUsed -> {
                val total = e.stolenFrom.values.sum()
                val deltas = mutableMapOf<PlayerId, ResourceCount>()
                if (total > 0) deltas[e.player] = ResourceCount.of(e.resource to total)
                for ((victim, amount) in e.stolenFrom) {
                    deltas[victim] = (deltas[victim] ?: ResourceCount()) - ResourceCount.of(e.resource to amount)
                }
                s.state.applyResourceDeltas(deltas)
            }
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
