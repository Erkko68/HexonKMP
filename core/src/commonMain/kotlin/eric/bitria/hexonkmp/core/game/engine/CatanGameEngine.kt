package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.action.BuyDevCard
import eric.bitria.hexonkmp.core.game.action.CancelTrade
import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.PlayKnight
import eric.bitria.hexonkmp.core.game.action.PlayMonopoly
import eric.bitria.hexonkmp.core.game.action.PlayRoadBuilding
import eric.bitria.hexonkmp.core.game.action.PlayYearOfPlenty
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.action.StealFrom
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.board.BoardGenerator
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.event.BuildingPlaced
import eric.bitria.hexonkmp.core.game.event.CityUpgraded
import eric.bitria.hexonkmp.core.game.event.DevCardBought
import eric.bitria.hexonkmp.core.game.event.DevCardPlayed
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.GameEnded
import eric.bitria.hexonkmp.core.game.event.LargestArmyChanged
import eric.bitria.hexonkmp.core.game.event.LongestRoadChanged
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.event.MonopolyUsed
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
import eric.bitria.hexonkmp.core.game.event.YearOfPlentyUsed
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

// The result type for the Catan engine: GameResult bound to Catan's state/event.
// A local alias keeps the many reduce-helper signatures readable.
private typealias CatanResult = GameResult<GameState, GameEvent>

// The pure Catan game engine. Every rule lives behind reduce(); it has no concept
// of sockets, coroutines, or connected players. Same code runs on the server (as
// the source of truth) and on the client (to pre-validate before sending).
// Implements CatanEngine: the generic transport contract (GameEngine) bound to
// Catan types, plus the legal-move queries the client UI uses.
//
// Driven by a ScenarioConfig — swap the config to get a different game mode
// without touching this class. `boardSeed` makes board generation deterministic
// (override in tests for a fixed layout).
class CatanGameEngine(
    private val config: ScenarioConfig = ClassicCatan,
    private val boardSeed: Long = Random.nextLong(),
) : CatanEngine {

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

    override fun reduce(state: GameState, actor: PlayerId, action: GameAction): CatanResult {
        // A couple of actions are taken by non-current players: responding to a
        // trade offer, and discarding for a 7. Handle them before the turn guard.
        if (action is RespondTrade) return respondTrade(state, actor, action)
        if (action is DiscardResources) return discardResources(state, actor, action.cards)
        if (actor != state.currentPlayer) {
            return CatanResult(state, rejection = "It is not your turn")
        }
        return when (action) {
            EndTurn -> endTurn(state)
            is PlaceSettlement ->
                if (state.phase is GamePhase.Setup) placeSettlement(state, actor, action.vertex)
                else buildSettlement(state, actor, action.vertex)
            is PlaceRoad -> when (state.phase) {
                is GamePhase.Setup -> placeRoad(state, actor, action.edge)
                is GamePhase.RoadBuilding -> placeRoadFree(state, actor, action.edge)
                else -> buildRoad(state, actor, action.edge)
            }
            is UpgradeCity -> upgradeCity(state, actor, action.vertex)
            is MoveRobber -> moveRobber(state, actor, action.hex)
            is StealFrom -> stealFrom(state, actor, action.target)
            BuyDevCard -> buyDevCard(state, actor)
            PlayKnight -> playKnight(state, actor)
            PlayRoadBuilding -> playRoadBuilding(state, actor)
            is PlayYearOfPlenty -> playYearOfPlenty(state, actor, action.resources)
            is PlayMonopoly -> playMonopoly(state, actor, action.resource)
            is BankTrade -> bankTrade(state, actor, action.give, action.receive)
            is ProposeTrade -> proposeTrade(state, actor, action.give, action.receive)
            is FinalizeTrade -> finalizeTrade(state, actor, action.offerId, action.partner)
            is CancelTrade -> cancelTrade(state, actor, action.offerId)
            is RespondTrade -> respondTrade(state, actor, action) // unreachable; handled above
            is DiscardResources -> discardResources(state, actor, action.cards) // unreachable; handled above
        }
    }

    // --- Play: bank trade (give/receive, validated against the player's ratios) ---

    // A bank trade is "give X, receive Y" where the bank funds each received card
    // from the given cards at the player's per-resource ratio (4:1 by default,
    // lower with ports). Validation: give and receive disjoint; each given resource
    // an exact multiple of its ratio; the funded outputs exactly equal receive.total.
    private fun bankTrade(
        state: GameState,
        actor: PlayerId,
        give: ResourceCount,
        receive: ResourceCount,
    ): CatanResult {
        bankTradeRejection(state, actor, give, receive)?.let { return CatanResult(state, rejection = it) }
        val next = state.copy(
            hands = addGain(spend(state.hands, actor, give), actor, receive),
        )
        return CatanResult(next, events = listOf(BankTraded(actor, give, receive)))
    }

    // Validates a give/receive bank trade against the player's per-resource ratios.
    // give and receive must be disjoint; the hand must cover give; each given
    // resource must be an exact multiple of its ratio; and the outputs those
    // multiples fund must exactly equal receive.total. Shared by reduce() and UI.
    override fun bankTradeRejection(
        state: GameState,
        player: PlayerId,
        give: ResourceCount,
        receive: ResourceCount,
    ): String? {
        if (state.phase !is GamePhase.Play) return "You can only trade during your turn"
        if (give.isEmpty || receive.isEmpty) return "Nothing to trade"
        if (give.amounts.keys.any { it in receive.amounts.keys }) {
            return "Trade must be for a different resource"
        }
        if (!state.handOf(player).covers(give)) return "Not enough resources for this trade"
        val rates = bankRates(state, player)
        var funded = 0
        for ((res, qty) in give.amounts) {
            val ratio = rates.getValue(res)
            if (qty % ratio != 0) return "Give $res in multiples of $ratio"
            funded += qty / ratio
        }
        if (funded != receive.total) return "The bank rate doesn't balance this trade"
        return null
    }

    // The player's best (lowest) bank ratio per resource: the base ratio lowered by
    // any harbor with one of the player's buildings on EITHER of its edge's vertices.
    // A generic port (resource == null) lowers every resource; a specific port lowers
    // only its own.
    override fun bankRates(state: GameState, player: PlayerId): Map<Resource, Int> {
        val base = state.config.rules.bankTradeRatio
        val rates = Resource.entries.associateWith { base }.toMutableMap()
        val owned = state.buildings.filter { it.owner == player }.map { it.vertex }.toSet()
        for (port in state.board.ports) {
            if (port.edge.endpoints().none { it in owned }) continue
            if (port.resource == null) {
                for (res in Resource.entries) rates[res] = minOf(rates.getValue(res), port.ratio)
            } else {
                rates[port.resource] = minOf(rates.getValue(port.resource), port.ratio)
            }
        }
        return rates
    }

    // --- Play: player-to-player trades (propose -> respond -> finalize) ---

    // The current player offers [give] for [receive], broadcast to all opponents.
    private fun proposeTrade(
        state: GameState,
        actor: PlayerId,
        give: ResourceCount,
        receive: ResourceCount,
    ): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only trade during your turn")
        }
        if (give.isEmpty || receive.isEmpty) {
            return CatanResult(state, rejection = "A trade must offer and request something")
        }
        if (give.amounts.keys.any { it in receive.amounts.keys }) {
            return CatanResult(state, rejection = "Trade must be for different resources")
        }
        if (!state.handOf(actor).covers(give)) {
            return CatanResult(state, rejection = "You don't have those resources")
        }
        val offer = TradeOffer(id = state.tradeCounter, proposer = actor, give = give, receive = receive)
        val next = state.copy(
            pendingTrades = state.pendingTrades + offer,
            tradeCounter = state.tradeCounter + 1,
        )
        return CatanResult(next, events = listOf(TradeProposed(offer)))
    }

    // An opponent accepts/declines a pending offer. Allowed off-turn (the only
    // such action); accepting re-validates that the responder can cover [receive].
    private fun respondTrade(state: GameState, actor: PlayerId, action: RespondTrade): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "No trade to respond to")
        }
        val offer = state.pendingTrades.firstOrNull { it.id == action.offerId }
            ?: return CatanResult(state, rejection = "That offer is no longer available")
        if (actor == offer.proposer) {
            return CatanResult(state, rejection = "You can't respond to your own offer")
        }
        if (actor !in state.players || actor !in state.present) {
            return CatanResult(state, rejection = "Not a participant")
        }
        if (action.accept && !state.handOf(actor).covers(offer.receive)) {
            return CatanResult(state, rejection = "You don't have the resources to accept")
        }
        val updated = offer.copy(responses = offer.responses + (actor to action.accept))
        val next = state.copy(
            pendingTrades = state.pendingTrades.map { if (it.id == offer.id) updated else it },
        )
        return CatanResult(next, events = listOf(TradeResponded(offer.id, actor, action.accept)))
    }

    // The proposer finalizes an offer with one accepter. Both hands are
    // re-validated (they may have changed since the response), then swapped, and
    // ALL pending offers are cleared.
    private fun finalizeTrade(
        state: GameState,
        actor: PlayerId,
        offerId: Int,
        partner: PlayerId,
    ): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only trade during your turn")
        }
        val offer = state.pendingTrades.firstOrNull { it.id == offerId }
            ?: return CatanResult(state, rejection = "That offer is no longer available")
        if (actor != offer.proposer) {
            return CatanResult(state, rejection = "Only the proposer can finalize this trade")
        }
        if (partner == actor || offer.responses[partner] != true) {
            return CatanResult(state, rejection = "That player hasn't accepted")
        }
        if (partner !in state.present) {
            return CatanResult(state, rejection = "That player is no longer in the game")
        }
        // Re-validate both sides at finalize time (anti-cheat: hands can change).
        if (!state.handOf(actor).covers(offer.give)) {
            return CatanResult(state, rejection = "You no longer have those resources")
        }
        if (!state.handOf(partner).covers(offer.receive)) {
            return CatanResult(state, rejection = "That player no longer has the resources")
        }
        var hands = state.hands
        hands = addGain(spend(hands, actor, offer.give), actor, offer.receive)
        hands = addGain(spend(hands, partner, offer.receive), partner, offer.give)
        val next = state.copy(hands = hands, pendingTrades = emptyList())
        return CatanResult(
            next,
            events = listOf(TradeFinalized(offer.id, actor, partner, offer.give, offer.receive)),
        )
    }

    // The proposer withdraws one of their pending offers.
    private fun cancelTrade(state: GameState, actor: PlayerId, offerId: Int): CatanResult {
        val offer = state.pendingTrades.firstOrNull { it.id == offerId }
            ?: return CatanResult(state, rejection = "That offer is no longer available")
        if (actor != offer.proposer) {
            return CatanResult(state, rejection = "Only the proposer can cancel this offer")
        }
        val next = state.copy(pendingTrades = state.pendingTrades.filterNot { it.id == offerId })
        return CatanResult(next, events = listOf(TradeCancelled(offerId)))
    }

    // --- Play: building (costs resources, unlike free setup placement) ---

    private fun buildSettlement(state: GameState, actor: PlayerId, vertex: Vertex): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "Cannot build right now")
        }
        playSettlementRejection(state, actor, vertex)?.let { return CatanResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.SETTLEMENT)
        if (!state.handOf(actor).covers(cost)) {
            return CatanResult(state, rejection = "Not enough resources")
        }
        val building = Building(actor, vertex, Building.Kind.SETTLEMENT)
        val next = state.copy(
            buildings = state.buildings + building,
            hands = spend(state.hands, actor, cost),
        )
        return endIfWon(CatanResult(next, events = listOf(BuildingPlaced(building))), actor)
    }

    private fun buildRoad(state: GameState, actor: PlayerId, edge: Edge): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "Cannot build right now")
        }
        playRoadRejection(state, actor, edge)?.let { return CatanResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.ROAD)
        if (!state.handOf(actor).covers(cost)) {
            return CatanResult(state, rejection = "Not enough resources")
        }
        val road = Road(actor, edge)
        val placed = state.copy(
            roads = state.roads + road,
            hands = spend(state.hands, actor, cost),
        )
        return checkLongestRoad(CatanResult(placed, events = listOf(RoadPlaced(road))), actor)
    }

    private fun upgradeCity(state: GameState, actor: PlayerId, vertex: Vertex): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "Cannot build right now")
        }
        cityRejection(state, actor, vertex)?.let { return CatanResult(state, rejection = it) }
        val cost = state.config.rules.cost(Buildable.CITY)
        if (!state.handOf(actor).covers(cost)) {
            return CatanResult(state, rejection = "Not enough resources")
        }
        val city = Building(actor, vertex, Building.Kind.CITY)
        val next = state.copy(
            buildings = state.buildings.map { if (it.vertex == vertex) city else it },
            hands = spend(state.hands, actor, cost),
        )
        return endIfWon(CatanResult(next, events = listOf(CityUpgraded(city)), ), actor)
    }

    // --- Play: development cards ---

    // Buy the top card of the deck. Costs DEV_CARD; the card lands in the buyer's
    // boughtThisTurn bucket (not playable until next turn). A Victory-Point card can
    // win the game on purchase, so we run endIfWon. The drawn card is private — the
    // event is redacted to null for everyone but the buyer.
    private fun buyDevCard(state: GameState, actor: PlayerId): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only buy on your turn")
        }
        if (state.devDeck.isEmpty()) {
            return CatanResult(state, rejection = "No development cards left")
        }
        val cost = state.config.rules.cost(Buildable.DEV_CARD)
        if (!state.handOf(actor).covers(cost)) {
            return CatanResult(state, rejection = "Not enough resources")
        }
        val card = state.devDeck.first()
        val deck = state.devDeck.drop(1)
        val bought = state.boughtThisTurn + (actor to (state.boughtThisTurn[actor].orEmpty() + card))
        val next = state.copy(
            devDeck = deck,
            boughtThisTurn = bought,
            hands = spend(state.hands, actor, cost),
        )
        return endIfWon(CatanResult(next, events = listOf(DevCardBought(actor, card, deck.size))), actor)
    }

    // Play a Knight: spend a playable KNIGHT, count it toward Largest Army, and
    // enter the Robber phase (the player then issues MoveRobber to relocate+steal,
    // exactly like a 7 — but with no discard step). One dev card per turn.
    private fun playKnight(state: GameState, actor: PlayerId): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only play a card on your turn")
        }
        if (state.devCardPlayed) {
            return CatanResult(state, rejection = "You already played a development card this turn")
        }
        val playable = state.devCards[actor].orEmpty()
        if (DevCard.KNIGHT !in playable) {
            return CatanResult(state, rejection = "You have no Knight to play")
        }
        // Remove one knight from the playable hand.
        val remaining = playable.toMutableList().apply { remove(DevCard.KNIGHT) }
        val knights = state.knightsPlayed + (actor to (state.knightsPlayed[actor] ?: 0) + 1)
        var next = state.copy(
            devCards = state.devCards + (actor to remaining),
            knightsPlayed = knights,
            devCardPlayed = true,
            phase = GamePhase.Robber,
        )
        val events = mutableListOf<GameEvent>(
            DevCardPlayed(actor, DevCard.KNIGHT),
        )
        // Award Largest Army if this knight makes [actor] the strict leader past the
        // minimum threshold.
        val newHolder = largestArmyHolder(next)
        if (newHolder != next.largestArmy) {
            next = next.copy(largestArmy = newHolder)
            events += LargestArmyChanged(newHolder)
        }
        events += PhaseChanged(GamePhase.Robber)
        // Largest Army's points can reach the goal — end the game if so.
        return endIfWon(CatanResult(next, events = events), actor)
    }

    // Play a Road Building card: spend it, set devCardPlayed, enter RoadBuilding(2).
    // The player then issues two PlaceRoad actions (free, no cost) before the engine
    // automatically returns to Play.
    private fun playRoadBuilding(state: GameState, actor: PlayerId): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only play a card on your turn")
        }
        if (state.devCardPlayed) {
            return CatanResult(state, rejection = "You already played a development card this turn")
        }
        val playable = state.devCards[actor].orEmpty()
        if (DevCard.ROAD_BUILDING !in playable) {
            return CatanResult(state, rejection = "You have no Road Building card to play")
        }
        val remaining = playable.toMutableList().apply { remove(DevCard.ROAD_BUILDING) }
        val phase = GamePhase.RoadBuilding(roadsLeft = 2)
        val next = state.copy(
            devCards = state.devCards + (actor to remaining),
            devCardPlayed = true,
            phase = phase,
        )
        return CatanResult(next, events = listOf(DevCardPlayed(actor, DevCard.ROAD_BUILDING), PhaseChanged(phase)))
    }

    // Place one free road during the RoadBuilding phase. No cost check. Decrements
    // roadsLeft; returns to Play automatically when it reaches 0.
    private fun placeRoadFree(state: GameState, actor: PlayerId, edge: Edge): CatanResult {
        val phase = state.phase as? GamePhase.RoadBuilding
            ?: return CatanResult(state, rejection = "Not in Road Building phase")
        playRoadRejection(state, actor, edge)?.let { return CatanResult(state, rejection = it) }
        val road = Road(actor, edge)
        val roadsLeft = phase.roadsLeft - 1
        val nextPhase: GamePhase = if (roadsLeft > 0) GamePhase.RoadBuilding(roadsLeft) else GamePhase.Play
        val placed = state.copy(roads = state.roads + road, phase = nextPhase)
        return checkLongestRoad(
            CatanResult(placed, events = listOf(RoadPlaced(road), PhaseChanged(nextPhase))),
            actor,
        )
    }

    // Play a Year of Plenty card: take exactly 2 resources of the player's choice
    // from the (infinite) bank. Fully public — the event reveals both resources.
    private fun playYearOfPlenty(state: GameState, actor: PlayerId, resources: ResourceCount): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only play a card on your turn")
        }
        if (state.devCardPlayed) {
            return CatanResult(state, rejection = "You already played a development card this turn")
        }
        val playable = state.devCards[actor].orEmpty()
        if (DevCard.YEAR_OF_PLENTY !in playable) {
            return CatanResult(state, rejection = "You have no Year of Plenty card to play")
        }
        if (resources.total != 2) {
            return CatanResult(state, rejection = "You must take exactly 2 resources")
        }
        val remaining = playable.toMutableList().apply { remove(DevCard.YEAR_OF_PLENTY) }
        val next = state.copy(
            devCards = state.devCards + (actor to remaining),
            devCardPlayed = true,
            hands = addGain(state.hands, actor, resources),
        )
        return CatanResult(next, events = listOf(DevCardPlayed(actor, DevCard.YEAR_OF_PLENTY), YearOfPlentyUsed(actor, resources)))
    }

    // Play a Monopoly card: take every opponent's supply of [resource]. Fully
    // public — the event names the resource and each victim's loss.
    private fun playMonopoly(state: GameState, actor: PlayerId, resource: Resource): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "You can only play a card on your turn")
        }
        if (state.devCardPlayed) {
            return CatanResult(state, rejection = "You already played a development card this turn")
        }
        val playable = state.devCards[actor].orEmpty()
        if (DevCard.MONOPOLY !in playable) {
            return CatanResult(state, rejection = "You have no Monopoly card to play")
        }
        val remaining = playable.toMutableList().apply { remove(DevCard.MONOPOLY) }
        var hands = state.hands
        val stolenFrom = mutableMapOf<PlayerId, Int>()
        for (player in state.players) {
            if (player == actor) continue
            val amount = state.handOf(player)[resource]
            if (amount > 0) {
                hands = spend(hands, player, ResourceCount.of(resource to amount))
                stolenFrom[player] = amount
            }
        }
        val total = stolenFrom.values.sum()
        if (total > 0) hands = addGain(hands, actor, ResourceCount.of(resource to total))
        val next = state.copy(
            devCards = state.devCards + (actor to remaining),
            devCardPlayed = true,
            hands = hands,
        )
        return CatanResult(next, events = listOf(DevCardPlayed(actor, DevCard.MONOPOLY), MonopolyUsed(actor, resource, stolenFrom)))
    }

    // Whoever should hold Largest Army given knights played: the strict maximum once
    // it reaches the configured minimum. The incumbent keeps it on ties (you must
    // exceed, not match, to take it).
    private fun largestArmyHolder(state: GameState): PlayerId? {
        val min = state.config.rules.largestArmyMin
        val leadCount = state.largestArmy?.let { state.knightsPlayed[it] ?: 0 } ?: 0
        var holder = state.largestArmy
        var best = maxOf(leadCount, min - 1) // must reach min, and beat the incumbent
        for (player in state.players) {
            val n = state.knightsPlayed[player] ?: 0
            if (n >= min && n > best) {
                best = n
                holder = player
            }
        }
        return holder
    }

    // The length of the longest continuous road chain owned by [player].
    // Roads form a graph of vertices connected by edges; a chain is broken at any
    // vertex occupied by an opponent's settlement or city (you can't route through
    // another player's building). Uses DFS with edge-visited tracking over the
    // (bounded) road graph, which is small enough to be exhaustive.
    internal fun longestRoadOf(state: GameState, player: PlayerId): Int {
        val playerRoads = state.roads.filter { it.owner == player }
        if (playerRoads.isEmpty()) return 0

        val visited = mutableSetOf<Edge>()

        fun dfs(vertex: Vertex): Int {
            var best = 0
            for (road in playerRoads) {
                if (road.edge in visited) continue
                if (!road.edge.touches(vertex)) continue
                // Move along this road to its other endpoint.
                val next = road.edge.endpoints().firstOrNull { it != vertex } ?: continue
                // An opponent's building on [next] breaks the chain — we count the
                // road reaching [next] but cannot extend beyond it.
                val blocked = state.buildingAt(next)?.owner.let { it != null && it != player }
                visited += road.edge
                val extension = if (blocked) 0 else dfs(next)
                best = maxOf(best, 1 + extension)
                visited -= road.edge
            }
            return best
        }

        // Try every endpoint of every player road as a starting point.
        var max = 0
        val startVertices = playerRoads.flatMap { it.edge.endpoints() }.distinct()
        for (v in startVertices) {
            max = maxOf(max, dfs(v))
        }
        return max
    }

    // Determines who should hold Longest Road: the player with the longest chain
    // at or above [longestRoadMin] roads. The incumbent keeps the title on ties
    // (challenger must strictly exceed the current holder's length).
    private fun longestRoadHolder(state: GameState): PlayerId? {
        val min = state.config.rules.longestRoadMin
        val leadLength = state.longestRoad?.let { longestRoadOf(state, it) } ?: 0
        var holder = state.longestRoad
        var best = maxOf(leadLength, min - 1) // must reach min, and beat the incumbent
        for (player in state.players) {
            val n = longestRoadOf(state, player)
            if (n >= min && n > best) {
                best = n
                holder = player
            }
        }
        return holder
    }

    // Checks whether Longest Road changed after a road was placed and, if so,
    // updates the state, emits LongestRoadChanged, and runs the win check.
    private fun checkLongestRoad(result: CatanResult, actor: PlayerId): CatanResult {
        val newHolder = longestRoadHolder(result.state)
        if (newHolder == result.state.longestRoad) return result
        val next = result.state.copy(longestRoad = newHolder)
        return endIfWon(
            result.copy(state = next, events = result.events + LongestRoadChanged(newHolder)),
            actor,
        )
    }

    private fun victoryPoints(state: GameState, player: PlayerId): Int {
        val fromBuildings = state.buildings.filter { it.owner == player }
            .sumOf { if (it.kind == Building.Kind.CITY) 2 else 1 }
        val fromVpCards = state.allDevCardsOf(player).count { it == DevCard.VICTORY_POINT }
        val fromArmy = if (state.largestArmy == player) state.config.rules.largestArmyVp else 0
        val fromRoad = if (state.longestRoad == player) state.config.rules.longestRoadVp else 0
        return fromBuildings + fromVpCards + fromArmy + fromRoad
    }

    // If [actor] just reached the victory goal, end the game: switch to Finished
    // and append a GameEnded event. Otherwise return the result unchanged.
    private fun endIfWon(result: CatanResult, actor: PlayerId): CatanResult {
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
    private fun discardResources(state: GameState, actor: PlayerId, cards: ResourceCount): CatanResult {
        val phase = state.phase as? GamePhase.Discard
            ?: return CatanResult(state, rejection = "Nothing to discard right now")
        val owed = phase.pending[actor]
            ?: return CatanResult(state, rejection = "You don't need to discard")
        if (cards.total != owed) {
            return CatanResult(state, rejection = "Discard exactly $owed card(s)")
        }
        if (!state.handOf(actor).covers(cards)) {
            return CatanResult(state, rejection = "You don't have those cards")
        }
        val spent = state.copy(hands = spend(state.hands, actor, cards))
        val remaining = phase.pending - actor
        val discarded = ResourcesDiscarded(actor, cards)
        // Still waiting on a present player -> stay in Discard with the new tally.
        if (remaining.keys.any { it in state.present }) {
            val next = spent.copy(phase = GamePhase.Discard(remaining))
            return CatanResult(next, events = listOf(discarded, PhaseChanged(next.phase)))
        }
        // All present owers done -> proceed to the robber move.
        val onward = enterRobberOrSkip(spent)
        return CatanResult(onward.state, events = listOf(discarded) + onward.events)
    }

    // Leave the discard step: the roller moves the robber if present, otherwise we
    // skip the robber and advance the turn (don't stall on an absent roller).
    private fun enterRobberOrSkip(state: GameState): CatanResult {
        return if (state.currentPlayer in state.present) {
            val next = state.copy(phase = GamePhase.Robber)
            CatanResult(next, events = listOf(PhaseChanged(GamePhase.Robber)))
        } else {
            advanceTurn(state.copy(phase = GamePhase.Play))
        }
    }

    private fun moveRobber(state: GameState, actor: PlayerId, hex: Axial): CatanResult {
        if (state.phase != GamePhase.Robber) {
            return CatanResult(state, rejection = "There's no robber to move")
        }
        if (state.board.tileAt(hex) == null) {
            return CatanResult(state, rejection = "Not a valid tile")
        }
        if (hex == state.board.robber) {
            return CatanResult(state, rejection = "Move the robber to a different tile")
        }
        val moved = state.copy(board = state.board.copy(robber = hex))
        val victims = state.buildings
            .filter { hex in it.vertex.hexes && it.owner != actor }
            .map { it.owner }
            .distinct()
            .filter { !state.handOf(it).isEmpty }

        return when {
            victims.isEmpty() -> {
                val next = moved.copy(phase = GamePhase.Play)
                CatanResult(next, events = listOf(RobberMoved(hex), PhaseChanged(GamePhase.Play)))
            }
            victims.size == 1 -> {
                // Single eligible victim: auto-steal (no UI selection needed).
                // RobberMoved + steal events are emitted together.
                executeSteal(moved, actor, victims.single(), leadEvents = listOf(RobberMoved(hex)))
            }
            else -> {
                // Multiple eligible victims: roller must choose — enter a sub-phase.
                val choosePhase = GamePhase.ChooseStealTarget(victims)
                val next = moved.copy(phase = choosePhase)
                CatanResult(next, events = listOf(RobberMoved(hex), PhaseChanged(choosePhase)))
            }
        }
    }

    // Steals one random card from [victim], returns to Play. [leadEvents] are
    // prepended to the result (used in the single-victim path to include RobberMoved).
    private fun executeSteal(
        state: GameState,
        actor: PlayerId,
        victim: PlayerId,
        leadEvents: List<GameEvent> = emptyList(),
    ): CatanResult {
        val rng = Random(state.rngSeed)
        val pool = state.handOf(victim).amounts.flatMap { (res, n) -> List(n) { res } }
        val stolen = pool[rng.nextInt(pool.size)]
        val one = ResourceCount.of(stolen to 1)
        val next = state.copy(
            hands = addGain(spend(state.hands, victim, one), actor, one),
            phase = GamePhase.Play,
            rngSeed = rng.nextLong(),
        )
        return CatanResult(
            next,
            events = leadEvents + PhaseChanged(GamePhase.Play) + ResourceStolen(victim, actor, stolen),
        )
    }

    private fun stealFrom(state: GameState, actor: PlayerId, target: PlayerId): CatanResult {
        val phase = state.phase as? GamePhase.ChooseStealTarget
            ?: return CatanResult(state, rejection = "Not choosing a steal target right now")
        if (target !in phase.victims) {
            return CatanResult(state, rejection = "That player is not an eligible target")
        }
        if (state.handOf(target).isEmpty) {
            return CatanResult(state, rejection = "That player has no cards to steal")
        }
        return executeSteal(state, actor, target)
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

    private fun placeSettlement(state: GameState, actor: PlayerId, vertex: Vertex): CatanResult {
        val setup = state.phase as? GamePhase.Setup
            ?: return CatanResult(state, rejection = "Settlements can only be placed during setup")
        if (setup.awaiting != Placement.SETTLEMENT) {
            return CatanResult(state, rejection = "You must place a road now")
        }
        settlementRejection(state, vertex)?.let { return CatanResult(state, rejection = it) }

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
        return CatanResult(next, events = events)
    }

    // --- Setup: road placement (completes a step, then advances the draft) ---

    private fun placeRoad(state: GameState, actor: PlayerId, edge: Edge): CatanResult {
        val setup = state.phase as? GamePhase.Setup
            ?: return CatanResult(state, rejection = "Roads can only be placed during setup")
        if (setup.awaiting != Placement.ROAD) {
            return CatanResult(state, rejection = "You must place a settlement now")
        }
        roadRejection(state, setup, edge)?.let { return CatanResult(state, rejection = it) }

        val road = Road(actor, edge)
        val placed = state.copy(roads = state.roads + road)
        val advance = advanceSetup(placed, setup)
        return CatanResult(advance.state, events = listOf(RoadPlaced(road)) + advance.events)
    }

    // Moves the draft to the next placement, or starts Play when the snake is
    // exhausted. Any slot belonging to an absent player is filled in for them with
    // a random legal settlement + road (rather than skipped) so the leaver still
    // gets a board presence and their second-round starting resources — important
    // if they later rejoin. DEADLOCK GUARD: the draft only ever comes to rest on a
    // *present* player (or transitions to Play); it never stops held by a leaver,
    // which would stall setup with nobody able to act.
    private fun advanceSetup(state: GameState, setup: GamePhase.Setup): CatanResult {
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
            return CatanResult(play.state, events + play.events)
        }

        val nextPhase = setup.copy(index = i, awaiting = Placement.SETTLEMENT, lastSettlement = null)
        val moved = s.copy(
            phase = nextPhase,
            currentPlayerIndex = setup.order[i],
        )
        events += PhaseChanged(nextPhase)
        events += TurnChanged(moved.currentPlayer, moved.turn)
        return CatanResult(moved, events)
    }

    // Setup is over: switch to Play and begin the first present player's turn
    // (which rolls the dice automatically).
    private fun startPlay(state: GameState): CatanResult {
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
        return CatanResult(begun.state, events = lead + begun.events)
    }

    // --- Play: end turn ---

    private fun endTurn(state: GameState): CatanResult {
        if (state.phase !is GamePhase.Play) {
            return CatanResult(state, rejection = "Finish placing your starting pieces first")
        }
        return advanceTurn(state)
    }

    override fun playerLeft(state: GameState, playerId: PlayerId): CatanResult {
        if (playerId !in state.present) return CatanResult(state)
        val base = state.copy(present = state.present - playerId)
        // Last player standing wins: once everyone else has left an in-progress game,
        // end it immediately with the sole survivor as winner (mirrors a normal win).
        if (base.phase !is GamePhase.Finished && base.present.size == 1) {
            val winner = base.present.first()
            return CatanResult(base.copy(phase = GamePhase.Finished(winner)), events = listOf(GameEnded(winner)))
        }
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
                CatanResult(next, events)
            } else {
                val onward = enterRobberOrSkip(s)
                CatanResult(onward.state, events + onward.events)
            }
        }
        if (state.currentPlayer != playerId) return CatanResult(base)
        // The current player left — keep the game moving.
        return when (val phase = base.phase) {
            is GamePhase.Play -> advanceTurn(base)
            // Auto-place the leaver's remaining setup piece(s) at random instead of
            // skipping, then advance the draft. advanceSetup never comes to rest on
            // an absent player, so this can't deadlock with the leaver holding state.
            is GamePhase.Setup -> {
                val filled = autoPlaceSetupSlot(base, phase, playerId)
                val advanced = advanceSetup(filled.state, phase)
                CatanResult(advanced.state, filled.events + advanced.events)
            }
            // Left mid-robber-move: just move on (robber stays put).
            is GamePhase.Robber -> advanceTurn(base.copy(phase = GamePhase.Play))
            // Left mid-steal-choice: the roller never picked, so choose a victim at
            // random on their behalf (rather than forfeit the steal), then advance.
            is GamePhase.ChooseStealTarget -> {
                val eligible = phase.victims.filter { !base.handOf(it).isEmpty }
                if (eligible.isEmpty()) {
                    advanceTurn(base.copy(phase = GamePhase.Play))
                } else {
                    val rng = Random(base.rngSeed)
                    val victim = eligible[rng.nextInt(eligible.size)]
                    val stolen = executeSteal(base.copy(rngSeed = rng.nextLong()), playerId, victim)
                    val advanced = advanceTurn(stolen.state)
                    CatanResult(advanced.state, stolen.events + advanced.events)
                }
            }
            // Left mid-road-building: forfeit remaining free roads and advance.
            is GamePhase.RoadBuilding -> advanceTurn(base.copy(phase = GamePhase.Play))
            // Handled above, but the when must stay exhaustive.
            is GamePhase.Discard -> CatanResult(base)
            // Game's over; nothing to advance.
            is GamePhase.Finished -> CatanResult(base)
        }
    }

    override fun playerJoined(state: GameState, playerId: PlayerId): CatanResult {
        if (playerId !in state.players || playerId in state.present) return CatanResult(state)
        return CatanResult(state.copy(present = state.present + playerId))
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
            GamePhase.Robber, is GamePhase.ChooseStealTarget,
            is GamePhase.Discard, is GamePhase.RoadBuilding, is GamePhase.Finished -> emptySet()
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
            is GamePhase.RoadBuilding ->
                state.board.edges().filter { playRoadRejection(state, player, it) == null }.toSet()
            GamePhase.Robber, is GamePhase.ChooseStealTarget,
            is GamePhase.Discard, is GamePhase.Finished -> emptySet()
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
    ): CatanResult {
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
        return CatanResult(s, events)
    }

    // --- Play: turn rotation + automatic roll ---

    // Moves the turn to the next *present* player after the current index,
    // skipping anyone who has left. Increments the turn counter when the seating
    // wraps past the end. No-op if nobody else is present. The new player's turn
    // begins immediately with an automatic roll (TurnChanged + roll events).
    private fun advanceTurn(state: GameState): CatanResult {
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
                return CatanResult(begun.state, events = lead + begun.events)
            }
        }
        // Only the current player (or nobody) is present — nothing to advance to.
        return CatanResult(state)
    }

    // Begins the current player's turn: matures any dev cards they bought last turn
    // into their playable hand, clears the one-dev-card-per-turn flag, then rolls the
    // dice automatically (digital game — no manual roll step) and distributes
    // resources. Pure: randomness is driven by state.rngSeed, which advances so the
    // next roll differs.
    private fun beginTurn(stateIn: GameState): CatanResult {
        // Graduate the current player's bought-this-turn cards to playable, and
        // reset the per-turn dev-card limit. (A player only buys on their own turn,
        // so by their next turn those cards are eligible to play.)
        val player = stateIn.currentPlayer
        val matured = stateIn.boughtThisTurn[player].orEmpty()
        val state = stateIn.copy(
            devCards = if (matured.isEmpty()) stateIn.devCards
            else stateIn.devCards + (player to (stateIn.devCards[player].orEmpty() + matured)),
            boughtThisTurn = if (matured.isEmpty()) stateIn.boughtThisTurn
            else stateIn.boughtThisTurn - player,
            devCardPlayed = false,
        )
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
            return CatanResult(rolled, events = events)
        }

        val gains = produce(state, total)
        val newHands = applyGains(state.hands, gains)
        val rolled = state.copy(hands = newHands, lastRoll = total, rngSeed = nextSeed)
        val events = buildList {
            add(DiceRolled(die1, die2, total))
            if (gains.isNotEmpty()) add(ResourcesProduced(gains))
        }
        return CatanResult(rolled, events = events)
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
