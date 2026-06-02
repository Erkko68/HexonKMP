package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.board.BoardGenerator
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.PhaseChanged
import eric.bitria.hexonkmp.core.game.event.ResourcesProduced
import eric.bitria.hexonkmp.core.game.event.RoadPlaced
import eric.bitria.hexonkmp.core.game.event.TradeFinalized
import eric.bitria.hexonkmp.core.game.event.TradeOffersCleared
import eric.bitria.hexonkmp.core.game.event.TradeProposed
import eric.bitria.hexonkmp.core.game.event.TradeResponded
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.Placement
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.Road
import eric.bitria.hexonkmp.core.game.model.TradeOffer
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
            rngSeed = boardSeed,
        )
        // No dice in setup — the first roll happens when Play begins.
    }

    override fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult {
        // Responding to a trade offer is the one action an opponent (a
        // non-current player) may take during someone else's turn.
        if (action is RespondTrade) return respondTrade(state, actor, action)
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
            is BankTrade -> bankTrade(state, actor, action.swaps)
            is ProposeTrade -> proposeTrade(state, actor, action.give, action.receive)
            is FinalizeTrade -> finalizeTrade(state, actor, action.offerId, action.partner)
            is RespondTrade -> respondTrade(state, actor, action) // unreachable; handled above
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
        return GameResult(next, events = listOf(BuildingPlaced(building)))
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

    // Moves the draft to the next present player's placement, or starts Play when
    // the snake is exhausted.
    private fun advanceSetup(state: GameState, setup: GamePhase.Setup): GameResult {
        var i = setup.index + 1
        while (i < setup.order.size && state.players[setup.order[i]] !in state.present) i++
        if (i >= setup.order.size) return startPlay(state)

        val nextPhase = setup.copy(index = i, awaiting = Placement.SETTLEMENT, lastSettlement = null)
        val moved = state.copy(
            phase = nextPhase,
            currentPlayerIndex = setup.order[i],
        )
        return GameResult(
            moved,
            events = listOf(PhaseChanged(nextPhase), TurnChanged(moved.currentPlayer, moved.turn)),
        )
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
        if (state.currentPlayer != playerId) return GameResult(base)
        // The current player left — keep the game moving.
        return when (val phase = base.phase) {
            is GamePhase.Play -> advanceTurn(base)
            // Skip the leaver's remaining setup placement(s).
            is GamePhase.Setup -> skipSetup(base, phase)
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
        }
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

    private fun skipSetup(state: GameState, setup: GamePhase.Setup): GameResult {
        var i = setup.index
        while (i < setup.order.size && state.players[setup.order[i]] !in state.present) i++
        if (i >= setup.order.size) return startPlay(state)
        val nextPhase = setup.copy(index = i, awaiting = Placement.SETTLEMENT, lastSettlement = null)
        val moved = state.copy(
            phase = nextPhase,
            currentPlayerIndex = setup.order[i],
        )
        return GameResult(
            moved,
            events = listOf(PhaseChanged(nextPhase), TurnChanged(moved.currentPlayer, moved.turn)),
        )
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

        val gains = produce(state, total)
        val newHands = applyGains(state.hands, gains)

        val rolled = state.copy(
            hands = newHands,
            lastRoll = total,
            rngSeed = rng.nextLong(), // advance so the next turn rolls differently
        )
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
}
