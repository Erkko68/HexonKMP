package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.action.CancelTrade
import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.board.BoardGenerator
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.CityUpgraded
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.GameEnded
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.event.PhaseChanged
import eric.bitria.hexonkmp.core.game.event.ResourceStolen
import eric.bitria.hexonkmp.core.game.event.ResourcesDiscarded
import eric.bitria.hexonkmp.core.game.event.ResourcesProduced
import eric.bitria.hexonkmp.core.game.event.RoadPlaced
import eric.bitria.hexonkmp.core.game.event.RobberMoved
import eric.bitria.hexonkmp.core.game.event.TradeCancelled
import eric.bitria.hexonkmp.core.game.event.TradeFinalized
import eric.bitria.hexonkmp.core.game.event.TradeOffersCleared
import eric.bitria.hexonkmp.core.game.event.TradeProposed
import eric.bitria.hexonkmp.core.game.event.TradeResponded
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.Placement
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.Road
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.game.model.board.adjacentVertices
import eric.bitria.hexonkmp.core.game.model.board.endpoints
import eric.bitria.hexonkmp.core.game.model.board.incidentEdges
import eric.bitria.hexonkmp.core.game.model.board.touches
import kotlin.random.Random

// The pure game engine. Every rule lives behind reduce(); it has no concept of
// sockets, coroutines, or connected players. Same code runs on the server (as
// the source of truth) and on the client (to pre-validate before sending).
interface GameEngine {
    fun initialState(players: List<PlayerId>): GameState

    // (state, who acted, what they did) -> result. Must be a pure function:
    // no I/O, no shared mutable state, deterministic.
    fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult

    // Presence changes. A player leaving while it is their turn must hand the
    // turn to the next present player, otherwise the game stalls. A player
    // rejoining is simply marked present again — the turn order is unchanged.
    fun playerLeft(state: GameState, playerId: PlayerId): GameResult
    fun playerJoined(state: GameState, playerId: PlayerId): GameResult

    // Pure legal-move queries — what `player` may place right now. The UI uses
    // these to offer valid placements (and could highlight them on a board).
    fun legalSettlements(state: GameState, player: PlayerId): Set<Vertex>
    fun legalRoads(state: GameState, player: PlayerId): Set<Edge>

    // Vertices where `player` may upgrade a settlement to a city right now.
    fun legalCities(state: GameState, player: PlayerId): Set<Vertex>

    // Whether `player` can afford a buildable from their current hand. The UI
    // uses this to enable/disable build buttons during the Play phase.
    fun canAfford(state: GameState, player: PlayerId, buildable: Buildable): Boolean
}

// Generic Catan engine driven by a ScenarioConfig — swap the config to get a
// different game mode without touching this class. `boardSeed` makes board
// generation deterministic (override in tests for a fixed layout).
class CatanGameEngine(
    private val config: ScenarioConfig = ClassicCatan,
    private val boardSeed: Long = Random.nextLong(),
) : GameEngine {

    override fun initialState(players: List<PlayerId>): GameState {
        // Snake draft order: forward then reverse, e.g. 3 players -> 0,1,2,2,1,0.
        val forward = players.indices.toList()
        val order = forward + forward.reversed()
        return GameState(
            players = players,
            present = players.toSet(),
            config = config,
            board = BoardGenerator.generate(config, boardSeed),
            phase = GamePhase.Setup(order = order, index = 0, awaiting = Placement.SETTLEMENT),
            currentPlayerIndex = order.first(),
            devDeck = shuffledDevDeck(),
            rngSeed = boardSeed,
        )
        // No dice in setup — the first roll happens when Play begins.
    }

    // The shuffled development-card draw pile, deterministic from the board seed
    // (kept distinct from board generation so the two don't correlate). Cards are
    // expanded from the config's per-type counts.
    private fun shuffledDevDeck(): List<DevCard> =
        config.rules.devCardDeck
            .flatMap { (card, count) -> List(count) { card } }
            .shuffled(Random(boardSeed * 31 + 17))

    override fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult {
        // A couple of actions are taken by non-current players: responding to a
        // trade offer, and discarding for a 7. Handle them before the turn guard.
        if (action is RespondTrade) return respondTrade(state, actor, action)
        if (action is DiscardResources) return discardResources(state, actor, action.cards)
        if (actor != state.currentPlayer) {
            return GameResult(state, rejection = "It is not your turn")
        }
        return when (action) {
            EndTurn -> endTurn(state)
            is PlaceSettlement ->
                if (state.phase is GamePhase.Setup) placeSettlement(state, actor, action.vertex)
                else buildSettlement(state, actor, action.vertex)
            is PlaceRoad ->
                if (state.phase is GamePhase.Setup) placeRoad(state, actor, action.edge)
                else buildRoad(state, actor, action.edge)
            is UpgradeCity -> upgradeCity(state, actor, action.vertex)
            is MoveRobber -> moveRobber(state, actor, action.hex)
            is BankTrade -> bankTrade(state, actor, action.swaps)
            is ProposeTrade -> proposeTrade(state, actor, action.give, action.receive)
            is FinalizeTrade -> finalizeTrade(state, actor, action.offerId, action.partner)
            is CancelTrade -> cancelTrade(state, actor, action.offerId)
            is RespondTrade -> respondTrade(state, actor, action) // unreachable; handled above
            is DiscardResources -> discardResources(state, actor, action.cards) // unreachable; handled above
        }
    }

    // --- Play: bank trade (ratio:1 with the bank, applied atomically) ---

    private fun bankTrade(state: GameState, actor: PlayerId, swaps: List<BankSwap>): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "You can only trade during your turn")
        }
        if (swaps.isEmpty()) {
            return GameResult(state, rejection = "Nothing to trade")
        }
        if (swaps.any { it.give == it.get }) {
            return GameResult(state, rejection = "Trade must be for a different resource")
        }
        val ratio = state.config.rules.bankTradeRatio
        // Aggregate the whole trade: each swap costs `ratio` of its give-resource
        // and yields 1 of its get-resource. Summing first lets us validate the
        // total against the hand once, with no partial application.
        var given = ResourceCount()
        var received = ResourceCount()
        for (swap in swaps) {
            given += ResourceCount.of(swap.give to ratio)
            received += ResourceCount.of(swap.get to 1)
        }
        if (!state.handOf(actor).covers(given)) {
            return GameResult(state, rejection = "Not enough resources for this trade")
        }
        val next = state.copy(
            hands = addGain(spend(state.hands, actor, given), actor, received),
        )
        return GameResult(next, events = listOf(BankTraded(actor, given, received)))
    }

    // --- Play: player-to-player trades (propose -> respond -> finalize) ---

    // The current player offers [give] for [receive], broadcast to all opponents.
    private fun proposeTrade(
        state: GameState,
        actor: PlayerId,
        give: ResourceCount,
        receive: ResourceCount,
    ): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "You can only trade during your turn")
        }
        if (give.isEmpty || receive.isEmpty) {
            return GameResult(state, rejection = "A trade must offer and request something")
        }
        if (give.amounts.keys.any { it in receive.amounts.keys }) {
            return GameResult(state, rejection = "Trade must be for different resources")
        }
        if (!state.handOf(actor).covers(give)) {
            return GameResult(state, rejection = "You don't have those resources")
        }
        val offer = TradeOffer(id = state.tradeCounter, proposer = actor, give = give, receive = receive)
        val next = state.copy(
            pendingTrades = state.pendingTrades + offer,
            tradeCounter = state.tradeCounter + 1,
        )
        return GameResult(next, events = listOf(TradeProposed(offer)))
    }

    // An opponent accepts/declines a pending offer. Allowed off-turn (the only
    // such action); accepting re-validates that the responder can cover [receive].
    private fun respondTrade(state: GameState, actor: PlayerId, action: RespondTrade): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "No trade to respond to")
        }
        val offer = state.pendingTrades.firstOrNull { it.id == action.offerId }
            ?: return GameResult(state, rejection = "That offer is no longer available")
        if (actor == offer.proposer) {
            return GameResult(state, rejection = "You can't respond to your own offer")
        }
        if (actor !in state.players || actor !in state.present) {
            return GameResult(state, rejection = "Not a participant")
        }
        if (action.accept && !state.handOf(actor).covers(offer.receive)) {
            return GameResult(state, rejection = "You don't have the resources to accept")
        }
        val updated = offer.copy(responses = offer.responses + (actor to action.accept))
        val next = state.copy(
            pendingTrades = state.pendingTrades.map { if (it.id == offer.id) updated else it },
        )
        return GameResult(next, events = listOf(TradeResponded(offer.id, actor, action.accept)))
    }

    // The proposer finalizes an offer with one accepter. Both hands are
    // re-validated (they may have changed since the response), then swapped, and
    // ALL pending offers are cleared.
    private fun finalizeTrade(
        state: GameState,
        actor: PlayerId,
        offerId: Int,
        partner: PlayerId,
    ): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "You can only trade during your turn")
        }
        val offer = state.pendingTrades.firstOrNull { it.id == offerId }
            ?: return GameResult(state, rejection = "That offer is no longer available")
        if (actor != offer.proposer) {
            return GameResult(state, rejection = "Only the proposer can finalize this trade")
        }
        if (partner == actor || offer.responses[partner] != true) {
            return GameResult(state, rejection = "That player hasn't accepted")
        }
        if (partner !in state.present) {
            return GameResult(state, rejection = "That player is no longer in the game")
        }
        // Re-validate both sides at finalize time (anti-cheat: hands can change).
        if (!state.handOf(actor).covers(offer.give)) {
            return GameResult(state, rejection = "You no longer have those resources")
        }
        if (!state.handOf(partner).covers(offer.receive)) {
            return GameResult(state, rejection = "That player no longer has the resources")
        }
        var hands = state.hands
        hands = addGain(spend(hands, actor, offer.give), actor, offer.receive)
        hands = addGain(spend(hands, partner, offer.receive), partner, offer.give)
        val next = state.copy(hands = hands, pendingTrades = emptyList())
        return GameResult(
            next,
            events = listOf(TradeFinalized(offer.id, actor, partner, offer.give, offer.receive)),
        )
    }

    // The proposer withdraws one of their pending offers.
    private fun cancelTrade(state: GameState, actor: PlayerId, offerId: Int): GameResult {
        val offer = state.pendingTrades.firstOrNull { it.id == offerId }
            ?: return GameResult(state, rejection = "That offer is no longer available")
        if (actor != offer.proposer) {
            return GameResult(state, rejection = "Only the proposer can cancel this offer")
        }
        val next = state.copy(pendingTrades = state.pendingTrades.filterNot { it.id == offerId })
        return GameResult(next, events = listOf(TradeCancelled(offerId)))
    }

    // --- Play: building (costs resources, unlike free setup placement) ---

    private fun buildSettlement(state: GameState, actor: PlayerId, vertex: Vertex): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "Cannot build right now")
        }
        playSettlementRejection(state, actor, vertex)?.let { return GameResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.SETTLEMENT)
        if (!state.handOf(actor).covers(cost)) {
            return GameResult(state, rejection = "Not enough resources")
        }
        val building = Building(actor, vertex, Building.Kind.SETTLEMENT)
        val next = state.copy(
            buildings = state.buildings + building,
            hands = spend(state.hands, actor, cost),
        )
        return endIfWon(GameResult(next, events = listOf(BuildingPlaced(building))), actor)
    }

    private fun buildRoad(state: GameState, actor: PlayerId, edge: Edge): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "Cannot build right now")
        }
        playRoadRejection(state, actor, edge)?.let { return GameResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.ROAD)
        if (!state.handOf(actor).covers(cost)) {
            return GameResult(state, rejection = "Not enough resources")
        }
        val road = Road(actor, edge)
        val next = state.copy(
            roads = state.roads + road,
            hands = spend(state.hands, actor, cost),
        )
        return GameResult(next, events = listOf(RoadPlaced(road)))
    }

    private fun upgradeCity(state: GameState, actor: PlayerId, vertex: Vertex): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "Cannot build right now")
        }
        cityRejection(state, actor, vertex)?.let { return GameResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.CITY)
        if (!state.handOf(actor).covers(cost)) {
            return GameResult(state, rejection = "Not enough resources")
        }
        val city = Building(actor, vertex, Building.Kind.CITY)
        val next = state.copy(
            buildings = state.buildings.map { if (it.vertex == vertex) city else it },
            hands = spend(state.hands, actor, cost),
        )
        return endIfWon(GameResult(next, events = listOf(CityUpgraded(city)), ), actor)
    }

    private fun victoryPoints(state: GameState, player: PlayerId): Int =
        state.buildings.filter { it.owner == player }
            .sumOf { if (it.kind == Building.Kind.CITY) 2 else 1 }

    // If [actor] just reached the victory goal, end the game: switch to Finished
    // and append a GameEnded event. Otherwise return the result unchanged.
    private fun endIfWon(result: GameResult, actor: PlayerId): GameResult {
        if (victoryPoints(result.state, actor) < result.state.config.rules.victoryPointsToWin) return result
        return result.copy(
            state = result.state.copy(phase = GamePhase.Finished(actor)),
            events = result.events + GameEnded(actor),
        )
    }

    // --- Play: the robber (rolled a 7) ---

    // A player satisfies their 7 discard penalty. Allowed for any owing player
    // (current or not). Once no *present* player still owes, the robber moves
    // (or, if the roller is gone, the turn advances).
    private fun discardResources(state: GameState, actor: PlayerId, cards: ResourceCount): GameResult {
        val phase = state.phase as? GamePhase.Discard
            ?: return GameResult(state, rejection = "Nothing to discard right now")
        val owed = phase.pending[actor]
            ?: return GameResult(state, rejection = "You don't need to discard")
        if (cards.total != owed) {
            return GameResult(state, rejection = "Discard exactly $owed card(s)")
        }
        if (!state.handOf(actor).covers(cards)) {
            return GameResult(state, rejection = "You don't have those cards")
        }
        val spent = state.copy(hands = spend(state.hands, actor, cards))
        val remaining = phase.pending - actor
        val discarded = ResourcesDiscarded(actor, cards)
        // Still waiting on a present player -> stay in Discard with the new tally.
        if (remaining.keys.any { it in state.present }) {
            val next = spent.copy(phase = GamePhase.Discard(remaining))
            return GameResult(next, events = listOf(discarded, PhaseChanged(next.phase)))
        }
        // All present owers done -> proceed to the robber move.
        val onward = enterRobberOrSkip(spent)
        return GameResult(onward.state, events = listOf(discarded) + onward.events)
    }

    // Leave the discard step: the roller moves the robber if present, otherwise we
    // skip the robber and advance the turn (don't stall on an absent roller).
    private fun enterRobberOrSkip(state: GameState): GameResult {
        return if (state.currentPlayer in state.present) {
            val next = state.copy(phase = GamePhase.Robber)
            GameResult(next, events = listOf(PhaseChanged(GamePhase.Robber)))
        } else {
            advanceTurn(state.copy(phase = GamePhase.Play))
        }
    }

    private fun moveRobber(state: GameState, actor: PlayerId, hex: Axial): GameResult {
        if (state.phase != GamePhase.Robber) {
            return GameResult(state, rejection = "There's no robber to move")
        }
        if (state.board.tileAt(hex) == null) {
            return GameResult(state, rejection = "Not a valid tile")
        }
        if (hex == state.board.robber) {
            return GameResult(state, rejection = "Move the robber to a different tile")
        }
        val moved = state.copy(board = state.board.copy(robber = hex), phase = GamePhase.Play)
        // Steal one random card from a random opponent with a building on the tile.
        val victims = state.buildings
            .filter { hex in it.vertex.hexes && it.owner != actor }
            .map { it.owner }
            .distinct()
            .filter { !state.handOf(it).isEmpty }
        val events = mutableListOf<eric.bitria.hexonkmp.core.game.event.GameEvent>(
            RobberMoved(hex),
            PhaseChanged(GamePhase.Play),
        )
        if (victims.isEmpty()) {
            return GameResult(moved, events = events)
        }
        val rng = Random(state.rngSeed)
        val victim = victims[rng.nextInt(victims.size)]
        val pool = state.handOf(victim).amounts.flatMap { (res, n) -> List(n) { res } }
        val stolen = pool[rng.nextInt(pool.size)]
        val one = ResourceCount.of(stolen to 1)
        val next = moved.copy(
            hands = addGain(spend(moved.hands, victim, one), actor, one),
            rngSeed = rng.nextLong(),
        )
        events += ResourceStolen(victim, actor, stolen)
        return GameResult(next, events = events)
    }

    // Returns null if `actor` may upgrade the settlement at `vertex`, else why not.
    private fun cityRejection(state: GameState, actor: PlayerId, vertex: Vertex): String? {
        val building = state.buildingAt(vertex)
        return when {
            building == null -> "Build a settlement here first"
            building.owner != actor -> "Not your settlement"
            building.kind != Building.Kind.SETTLEMENT -> "Already a city"
            else -> null
        }
    }

    // --- Setup: settlement placement ---

    private fun placeSettlement(state: GameState, actor: PlayerId, vertex: Vertex): GameResult {
        val setup = state.phase as? GamePhase.Setup
            ?: return GameResult(state, rejection = "Settlements can only be placed during setup")
        if (setup.awaiting != Placement.SETTLEMENT) {
            return GameResult(state, rejection = "You must place a road now")
        }
        settlementRejection(state, vertex)?.let { return GameResult(state, rejection = it) }

        val building = Building(actor, vertex, Building.Kind.SETTLEMENT)
        val nextPhase = setup.copy(awaiting = Placement.ROAD, lastSettlement = vertex)
        var next = state.copy(
            buildings = state.buildings + building,
            phase = nextPhase,
        )
        // PhaseChanged carries the new setup sub-phase (now awaiting a ROAD) so
        // the client updates which build card is enabled.
        val events = mutableListOf<eric.bitria.hexonkmp.core.game.event.GameEvent>(
            BuildingPlaced(building),
            PhaseChanged(nextPhase),
        )

        // Second-round settlements grant their adjacent tiles' resources.
        if (setup.isSecondRound) {
            val gain = startingResources(state, vertex)
            if (!gain.isEmpty) {
                next = next.copy(hands = addGain(next.hands, actor, gain))
                events += ResourcesProduced(mapOf(actor to gain))
            }
        }
        return GameResult(next, events = events)
    }

    // --- Setup: road placement (completes a step, then advances the draft) ---

    private fun placeRoad(state: GameState, actor: PlayerId, edge: Edge): GameResult {
        val setup = state.phase as? GamePhase.Setup
            ?: return GameResult(state, rejection = "Roads can only be placed during setup")
        if (setup.awaiting != Placement.ROAD) {
            return GameResult(state, rejection = "You must place a settlement now")
        }
        roadRejection(state, setup, edge)?.let { return GameResult(state, rejection = it) }

        val road = Road(actor, edge)
        val placed = state.copy(roads = state.roads + road)
        val advance = advanceSetup(placed, setup)
        return GameResult(advance.state, events = listOf(RoadPlaced(road)) + advance.events)
    }

    // Moves the draft to the next placement, or starts Play when the snake is
    // exhausted. Any slot belonging to an absent player is filled in for them with
    // a random legal settlement + road (rather than skipped) so the leaver still
    // gets a board presence and their second-round starting resources — important
    // if they later rejoin. DEADLOCK GUARD: the draft only ever comes to rest on a
    // *present* player (or transitions to Play); it never stops held by a leaver,
    // which would stall setup with nobody able to act.
    private fun advanceSetup(state: GameState, setup: GamePhase.Setup): GameResult {
        var s = state
        val events = mutableListOf<eric.bitria.hexonkmp.core.game.event.GameEvent>()
        var i = setup.index + 1
        while (i < setup.order.size) {
            val player = s.players[setup.order[i]]
            if (player in s.present) break
            // Absent player's turn in the draft: auto-place randomly for them.
            val slotPhase = setup.copy(index = i, awaiting = Placement.SETTLEMENT, lastSettlement = null)
            val filled = autoPlaceSetupSlot(s.copy(phase = slotPhase), slotPhase, player)
            s = filled.state
            events += filled.events
            i++
        }
        if (i >= setup.order.size) {
            val play = startPlay(s)
            return GameResult(play.state, events + play.events)
        }

        val nextPhase = setup.copy(index = i, awaiting = Placement.SETTLEMENT, lastSettlement = null)
        val moved = s.copy(
            phase = nextPhase,
            currentPlayerIndex = setup.order[i],
        )
        events += PhaseChanged(nextPhase)
        events += TurnChanged(moved.currentPlayer, moved.turn)
        return GameResult(moved, events)
    }

    // Setup is over: switch to Play and begin the first present player's turn
    // (which rolls the dice automatically).
    private fun startPlay(state: GameState): GameResult {
        val firstPresent = state.players.indexOfFirst { it in state.present }.coerceAtLeast(0)
        val playState = state.copy(
            phase = GamePhase.Play,
            currentPlayerIndex = firstPresent,
            turn = 1,
        )
        val begun = beginTurn(playState)
        val lead = listOf(
            PhaseChanged(GamePhase.Play),
            TurnChanged(begun.state.currentPlayer, begun.state.turn),
        )
        return GameResult(begun.state, events = lead + begun.events)
    }

    // --- Play: end turn ---

    private fun endTurn(state: GameState): GameResult {
        if (state.phase !is GamePhase.Play) {
            return GameResult(state, rejection = "Finish placing your starting pieces first")
        }
        return advanceTurn(state)
    }

    override fun playerLeft(state: GameState, playerId: PlayerId): GameResult {
        if (playerId !in state.present) return GameResult(state)
        val base = state.copy(present = state.present - playerId)
        // Discards never wait on an absent player: a leaver who still owes pays it
        // immediately (cards picked at random), then the phase proceeds once no
        // present player still owes.
        val discardPhase = base.phase
        if (discardPhase is GamePhase.Discard) {
            val events = mutableListOf<eric.bitria.hexonkmp.core.game.event.GameEvent>()
            var s = base
            var pending = discardPhase.pending
            pending[playerId]?.let { owed ->
                val (cards, seed) = randomCards(s.handOf(playerId), owed, s.rngSeed)
                s = s.copy(hands = spend(s.hands, playerId, cards), rngSeed = seed)
                pending = pending - playerId
                events += ResourcesDiscarded(playerId, cards)
            }
            return if (pending.keys.any { it in s.present }) {
                val next = s.copy(phase = GamePhase.Discard(pending))
                events += PhaseChanged(next.phase)
                GameResult(next, events)
            } else {
                val onward = enterRobberOrSkip(s)
                GameResult(onward.state, events + onward.events)
            }
        }
        if (state.currentPlayer != playerId) return GameResult(base)
        // The current player left — keep the game moving.
        return when (val phase = base.phase) {
            is GamePhase.Play -> advanceTurn(base)
            // Auto-place the leaver's remaining setup piece(s) at random instead of
            // skipping, then advance the draft. advanceSetup never comes to rest on
            // an absent player, so this can't deadlock with the leaver holding state.
            is GamePhase.Setup -> {
                val filled = autoPlaceSetupSlot(base, phase, playerId)
                val advanced = advanceSetup(filled.state, phase)
                GameResult(advanced.state, filled.events + advanced.events)
            }
            // Left mid-robber-move: just move on (robber stays put).
            is GamePhase.Robber -> advanceTurn(base.copy(phase = GamePhase.Play))
            // Handled above, but the when must stay exhaustive.
            is GamePhase.Discard -> GameResult(base)
            // Game's over; nothing to advance.
            is GamePhase.Finished -> GameResult(base)
        }
    }

    override fun playerJoined(state: GameState, playerId: PlayerId): GameResult {
        if (playerId !in state.players || playerId in state.present) return GameResult(state)
        return GameResult(state.copy(present = state.present + playerId))
    }

    override fun legalSettlements(state: GameState, player: PlayerId): Set<Vertex> {
        if (player != state.currentPlayer) return emptySet()
        return when (val phase = state.phase) {
            is GamePhase.Setup ->
                if (phase.awaiting != Placement.SETTLEMENT) emptySet()
                else state.board.vertices().filter { settlementRejection(state, it) == null }.toSet()
            GamePhase.Play ->
                if (!canAfford(state, player, Buildable.SETTLEMENT)) emptySet()
                else state.board.vertices().filter { playSettlementRejection(state, player, it) == null }.toSet()
            GamePhase.Robber, is GamePhase.Discard, is GamePhase.Finished -> emptySet()
        }
    }

    override fun legalRoads(state: GameState, player: PlayerId): Set<Edge> {
        if (player != state.currentPlayer) return emptySet()
        return when (val phase = state.phase) {
            is GamePhase.Setup ->
                if (phase.awaiting != Placement.ROAD) emptySet()
                else state.board.edges().filter { roadRejection(state, phase, it) == null }.toSet()
            GamePhase.Play ->
                if (!canAfford(state, player, Buildable.ROAD)) emptySet()
                else state.board.edges().filter { playRoadRejection(state, player, it) == null }.toSet()
            GamePhase.Robber, is GamePhase.Discard, is GamePhase.Finished -> emptySet()
        }
    }

    override fun legalCities(state: GameState, player: PlayerId): Set<Vertex> {
        if (player != state.currentPlayer || state.phase !is GamePhase.Play) return emptySet()
        if (!canAfford(state, player, Buildable.CITY)) return emptySet()
        return state.buildings
            .filter { it.owner == player && it.kind == Building.Kind.SETTLEMENT }
            .map { it.vertex }
            .toSet()
    }

    override fun canAfford(state: GameState, player: PlayerId, buildable: Buildable): Boolean =
        state.handOf(player).covers(state.config.rules.cost(buildable))

    // --- Validation (shared by reduce() and the legal-move queries) ---

    // Returns null if the settlement is legal, else the reason it isn't.
    private fun settlementRejection(state: GameState, vertex: Vertex): String? = when {
        vertex !in state.board.vertices() -> "Not a valid spot"
        state.buildingAt(vertex) != null -> "Already occupied"
        // Distance rule: no settlement on an adjacent vertex.
        vertex.adjacentVertices().any { state.buildingAt(it) != null } ->
            "Too close to another settlement"
        else -> null
    }

    private fun roadRejection(state: GameState, setup: GamePhase.Setup, edge: Edge): String? = when {
        edge !in state.board.edges() -> "Not a valid spot"
        state.roadAt(edge) != null -> "Already occupied"
        // In setup the road must connect to the settlement just placed.
        setup.lastSettlement?.let { edge.touches(it) } != true ->
            "Road must connect to your new settlement"
        else -> null
    }

    // --- Play-phase placement rules (settlement/road must connect to your network) ---

    private fun playSettlementRejection(state: GameState, actor: PlayerId, vertex: Vertex): String? {
        settlementRejection(state, vertex)?.let { return it } // base: on board, free, distance
        // Must connect to one of your own roads.
        val touchesOwnRoad = vertex.incidentEdges().any { e -> state.roadAt(e)?.owner == actor }
        return if (touchesOwnRoad) null else "Must connect to one of your roads"
    }

    private fun playRoadRejection(state: GameState, actor: PlayerId, edge: Edge): String? {
        when {
            edge !in state.board.edges() -> return "Not a valid spot"
            state.roadAt(edge) != null -> return "Already occupied"
        }
        // A road extends your network from one of its endpoint vertices. But an
        // opponent's building sitting on that vertex blocks the connection — you
        // cannot run a road through another player's settlement/city, even if your
        // own road reaches the far side of it.
        val connects = edge.endpoints().any { v ->
            val occupant = state.buildingAt(v)?.owner
            if (occupant != null && occupant != actor) return@any false // opponent blocks this corner
            val ownBuilding = occupant == actor
            val ownRoadAtVertex = state.roads.any { it.owner == actor && it.edge.touches(v) }
            ownBuilding || ownRoadAtVertex
        }
        return if (connects) null else "Must connect to your network"
    }

    private fun startingResources(state: GameState, vertex: Vertex): ResourceCount {
        var total = ResourceCount()
        for (tile in state.board.tiles) {
            if (tile.hex !in vertex.hexes) continue
            tile.terrain.resource?.let { total += ResourceCount.of(it to 1) }
        }
        return total
    }

    // Fills in the placement(s) still owed for [setup]'s current slot on behalf of
    // [player] (who has left), picking uniformly at random among the legal spots.
    // Honours the sub-phase: if a settlement was already placed (awaiting ROAD) we
    // only add the connecting road; otherwise we place a settlement first (granting
    // second-round resources, like a normal placement) and then its road. Pure:
    // randomness is seeded from state.rngSeed, which is advanced. If no legal spot
    // exists for a piece (not possible on a standard board) we simply omit it
    // rather than fail, so the draft can never deadlock on the leaver.
    private fun autoPlaceSetupSlot(
        state: GameState,
        setup: GamePhase.Setup,
        player: PlayerId,
    ): GameResult {
        var s = state
        var phase = setup
        val events = mutableListOf<eric.bitria.hexonkmp.core.game.event.GameEvent>()
        val rng = Random(s.rngSeed)

        if (phase.awaiting == Placement.SETTLEMENT) {
            val spots = s.board.vertices().filter { settlementRejection(s, it) == null }
            if (spots.isNotEmpty()) {
                val vertex = spots[rng.nextInt(spots.size)]
                val building = Building(player, vertex, Building.Kind.SETTLEMENT)
                phase = phase.copy(awaiting = Placement.ROAD, lastSettlement = vertex)
                s = s.copy(buildings = s.buildings + building, phase = phase)
                events += BuildingPlaced(building)
                events += PhaseChanged(phase)
                // Second-round settlements grant their adjacent tiles' resources.
                if (phase.isSecondRound) {
                    val gain = startingResources(s, vertex)
                    if (!gain.isEmpty) {
                        s = s.copy(hands = addGain(s.hands, player, gain))
                        events += ResourcesProduced(mapOf(player to gain))
                    }
                }
            }
        }

        val edges = s.board.edges().filter { roadRejection(s, phase, it) == null }
        if (edges.isNotEmpty()) {
            val edge = edges[rng.nextInt(edges.size)]
            val road = Road(player, edge)
            s = s.copy(roads = s.roads + road)
            events += RoadPlaced(road)
        }

        s = s.copy(rngSeed = rng.nextLong())
        return GameResult(s, events)
    }

    // --- Play: turn rotation + automatic roll ---

    // Moves the turn to the next *present* player after the current index,
    // skipping anyone who has left. Increments the turn counter when the seating
    // wraps past the end. No-op if nobody else is present. The new player's turn
    // begins immediately with an automatic roll (TurnChanged + roll events).
    private fun advanceTurn(state: GameState): GameResult {
        val size = state.players.size
        for (step in 1..size) {
            val candidate = (state.currentPlayerIndex + step) % size
            if (state.players[candidate] in state.present) {
                val wrapped = state.currentPlayerIndex + step >= size
                val nextTurn = if (wrapped) state.turn + 1 else state.turn
                // Pending trade offers live only for the proposer's turn — drop
                // them as the turn moves on.
                val hadOffers = state.pendingTrades.isNotEmpty()
                val moved = state.copy(
                    currentPlayerIndex = candidate,
                    turn = nextTurn,
                    pendingTrades = emptyList(),
                )
                val begun = beginTurn(moved)
                val turnChanged = TurnChanged(begun.state.currentPlayer, begun.state.turn)
                val lead = if (hadOffers) listOf(TradeOffersCleared, turnChanged) else listOf(turnChanged)
                return GameResult(begun.state, events = lead + begun.events)
            }
        }
        // Only the current player (or nobody) is present — nothing to advance to.
        return GameResult(state)
    }

    // Begins the current player's turn: rolls the dice automatically (digital
    // game — no manual roll step) and distributes resources. Pure: randomness is
    // driven by state.rngSeed, which advances so the next roll differs.
    private fun beginTurn(state: GameState): GameResult {
        val rng = Random(state.rngSeed)
        val die1 = rng.nextInt(1, 7)
        val die2 = rng.nextInt(1, 7)
        val total = die1 + die2
        val nextSeed = rng.nextLong() // advance so the next draw differs

        // A 7 produces nothing. Players holding too many cards must first discard
        // half (Discard phase); then the current player moves the robber. Absent
        // owers are auto-discarded right now (at random) so the phase only ever
        // waits on present players and never stalls on someone who already left.
        if (total == 7) {
            val threshold = state.config.rules.robberDiscardThreshold
            val owed = state.players.associateWith { state.handOf(it).total }
                .filterValues { it > threshold }
                .mapValues { (_, n) -> n / 2 }
            var hands = state.hands
            var seed = nextSeed
            val autoDiscards = mutableListOf<GameEvent>()
            val pending = LinkedHashMap<PlayerId, Int>()
            for ((player, count) in owed) {
                if (player in state.present) {
                    pending[player] = count
                } else {
                    val (cards, advanced) = randomCards(state.handOf(player), count, seed)
                    hands = spend(hands, player, cards)
                    seed = advanced
                    autoDiscards += ResourcesDiscarded(player, cards)
                }
            }
            val nextPhase = if (pending.isEmpty()) GamePhase.Robber else GamePhase.Discard(pending)
            val rolled = state.copy(hands = hands, lastRoll = 7, rngSeed = seed, phase = nextPhase)
            val events = listOf(DiceRolled(die1, die2, 7)) + autoDiscards + PhaseChanged(nextPhase)
            return GameResult(rolled, events = events)
        }

        val gains = produce(state, total)
        val newHands = applyGains(state.hands, gains)
        val rolled = state.copy(hands = newHands, lastRoll = total, rngSeed = nextSeed)
        val events = buildList {
            add(DiceRolled(die1, die2, total))
            if (gains.isNotEmpty()) add(ResourcesProduced(gains))
        }
        return GameResult(rolled, events = events)
    }

    // Resources produced by a roll: every building on a vertex touching a tile
    // whose token matches `total` collects that tile's resource (settlement x1,
    // city x2). A 7 produces nothing (robber roll). Pure.
    private fun produce(state: GameState, total: Int): Map<PlayerId, ResourceCount> {
        if (total == 7) return emptyMap()
        val gains = mutableMapOf<PlayerId, ResourceCount>()
        for (tile in state.board.tiles) {
            if (tile.token != total) continue
            if (tile.hex == state.board.robber) continue // robbed tile produces for nobody
            val resource = tile.terrain.resource ?: continue
            for (building in state.buildings) {
                if (tile.hex !in building.vertex.hexes) continue
                val amount = ResourceCount.of(resource to building.kind.yield)
                gains[building.owner] = (gains[building.owner] ?: ResourceCount()) + amount
            }
        }
        return gains
    }

    private fun applyGains(
        hands: Map<PlayerId, ResourceCount>,
        gains: Map<PlayerId, ResourceCount>,
    ): Map<PlayerId, ResourceCount> {
        if (gains.isEmpty()) return hands
        val merged = hands.toMutableMap()
        for ((player, gain) in gains) {
            merged[player] = (merged[player] ?: ResourceCount()) + gain
        }
        return merged
    }

    private fun addGain(
        hands: Map<PlayerId, ResourceCount>,
        player: PlayerId,
        gain: ResourceCount,
    ): Map<PlayerId, ResourceCount> {
        val merged = hands.toMutableMap()
        merged[player] = (merged[player] ?: ResourceCount()) + gain
        return merged
    }

    private fun spend(
        hands: Map<PlayerId, ResourceCount>,
        player: PlayerId,
        cost: ResourceCount,
    ): Map<PlayerId, ResourceCount> {
        val merged = hands.toMutableMap()
        merged[player] = (merged[player] ?: ResourceCount()) - cost
        return merged
    }

    // Picks [n] cards at random from [hand] without replacement (capped at the
    // hand size), returning them and the advanced rng seed. Pure (seeded).
    private fun randomCards(hand: ResourceCount, n: Int, seed: Long): Pair<ResourceCount, Long> {
        val pool = hand.amounts.flatMap { (res, c) -> List(c) { res } }.toMutableList()
        val rng = Random(seed)
        var picked = ResourceCount()
        repeat(minOf(n, pool.size)) {
            val res = pool.removeAt(rng.nextInt(pool.size))
            picked += ResourceCount.of(res to 1)
        }
        return picked to rng.nextLong()
    }
}
