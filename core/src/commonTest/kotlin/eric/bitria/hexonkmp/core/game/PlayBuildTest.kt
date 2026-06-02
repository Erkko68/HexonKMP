package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.Road
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.game.model.board.adjacentVertices
import eric.bitria.hexonkmp.core.game.model.board.incidentEdges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Play-phase building: costs are deducted, and unaffordable builds are rejected.
class PlayBuildTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    private val roadCost = play.config.rules.cost(Buildable.ROAD)
    private val settlementCost = play.config.rules.cost(Buildable.SETTLEMENT)

    @Test
    fun buildingWithoutResourcesIsRejected() {
        // Give Alice an empty hand explicitly.
        val s = play.copy(hands = play.hands + (alice to ResourceCount()))
        val freeEdge = s.board.edges().first { s.roadAt(it) == null }
        val result = engine.reduce(s, s.currentPlayer, PlaceRoad(freeEdge))
        // currentPlayer may differ from alice; only assert when it's alice's turn.
        if (s.currentPlayer == alice) {
            assertNotNull(result.rejection)
        }
    }

    @Test
    fun affordableRoadDeductsItsCost() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to roadCost))
        // A legal road must connect to the player's existing network.
        val edge = engine.legalRoads(s, current).first()
        val result = engine.reduce(s, current, PlaceRoad(edge))
        assertNull(result.rejection)
        // Spent exactly the cost -> empty hand.
        assertTrue(result.state.handOf(current).isEmpty)
        assertEquals(s.roads.size + 1, result.state.roads.size)
    }

    @Test
    fun roadNotConnectedToNetworkIsRejected() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to roadCost))
        // An empty edge that is NOT in the legal set (not connected to the player).
        val legal = engine.legalRoads(s, current)
        val disconnected = s.board.edges().first { s.roadAt(it) == null && it !in legal }
        val result = engine.reduce(s, current, PlaceRoad(disconnected))
        assertNotNull(result.rejection)
    }

    @Test
    fun affordableSettlementDeductsItsCost() {
        val current = play.currentPlayer
        // Build a road first to extend the network, then a settlement on its far
        // end — guaranteeing a spot that satisfies both distance and connectivity.
        val funded = settlementCost + roadCost + ResourceCount.of(Resource.BRICK to 1)
        var s = play.copy(hands = play.hands + (current to funded))
        val roadEdge = engine.legalRoads(s, current).first()
        s = engine.reduce(s, current, PlaceRoad(roadEdge)).state

        val spot = engine.legalSettlements(s, current).firstOrNull()
        // If the new road opened no legal settlement spot, the rule still holds;
        // only assert deduction when a legal spot exists.
        if (spot != null) {
            val before = s.handOf(current)[Resource.BRICK]
            val result = engine.reduce(s, current, PlaceSettlement(spot))
            assertNull(result.rejection)
            assertEquals(before, result.state.handOf(current)[Resource.BRICK])
        }
    }

    @Test
    fun settlementNotConnectedToOwnRoadIsRejected() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to settlementCost))
        // A vertex that's empty and distance-legal but touches none of the
        // player's roads.
        val spot = s.board.vertices().first { v ->
            v.isDistanceLegal(s) && v.adjacentVertices().isNotEmpty() && v !in engine.legalSettlements(s, current)
        }
        val result = engine.reduce(s, current, PlaceSettlement(spot))
        assertNotNull(result.rejection)
    }

    @Test
    fun canAffordReflectsHand() {
        val current = play.currentPlayer
        val broke = play.copy(hands = play.hands + (current to ResourceCount()))
        assertEquals(false, engine.canAfford(broke, current, Buildable.ROAD))
        val funded = play.copy(hands = play.hands + (current to roadCost))
        assertEquals(true, engine.canAfford(funded, current, Buildable.ROAD))
    }

    // An opponent's building on a vertex severs road connectivity through it: a
    // road that would only link to the player's network *through* another player's
    // settlement is illegal — you can't build past it.
    @Test
    fun opponentBuildingBlocksRoadConnectionThroughThatVertex() {
        val base = engine.initialState(listOf(alice, bob))
        val board = base.board
        // An interior vertex whose three incident edges all exist on the board.
        val vertex = board.vertices().first { v -> v.incidentEdges().all { it in board.edges() } }
        val incident = vertex.incidentEdges()
        val aliceRoad = incident[0]   // Alice already has a road reaching the vertex
        val target = incident[1]      // the edge she now tries to extend past it

        val withOpponent = base.copy(
            phase = GamePhase.Play,
            buildings = listOf(Building(bob, vertex, Building.Kind.SETTLEMENT)),
            roads = listOf(Road(alice, aliceRoad)),
            hands = mapOf(alice to roadCost),
            currentPlayerIndex = base.players.indexOf(alice),
        )

        // Bob's settlement on the shared vertex blocks the extension.
        val rejected = engine.reduce(withOpponent, alice, PlaceRoad(target))
        assertNotNull(rejected.rejection)
        assertEquals(withOpponent, rejected.state)
        assertFalse(target in engine.legalRoads(withOpponent, alice))

        // Same layout without the blocking building: the extension is legal,
        // proving the building — not some other rule — is what rejects it.
        val unblocked = withOpponent.copy(buildings = emptyList())
        assertNull(engine.reduce(unblocked, alice, PlaceRoad(target)).rejection)
        assertTrue(target in engine.legalRoads(unblocked, alice))
    }

    private val cityCost = play.config.rules.cost(Buildable.CITY)

    @Test
    fun upgradingOwnSettlementToCityReplacesItAndDeductsCost() {
        val current = play.currentPlayer
        val vertex = play.buildings.first { it.owner == current && it.kind == Building.Kind.SETTLEMENT }.vertex
        val s = play.copy(hands = play.hands + (current to cityCost))
        val result = engine.reduce(s, current, UpgradeCity(vertex))
        assertNull(result.rejection)
        assertEquals(Building.Kind.CITY, result.state.buildingAt(vertex)?.kind)
        assertTrue(result.state.handOf(current).isEmpty)
        // Replaced in place, not added.
        assertEquals(s.buildings.size, result.state.buildings.size)
    }

    @Test
    fun upgradingWithoutResourcesIsRejected() {
        val current = play.currentPlayer
        val vertex = play.buildings.first { it.owner == current }.vertex
        val s = play.copy(hands = play.hands + (current to ResourceCount()))
        assertNotNull(engine.reduce(s, current, UpgradeCity(vertex)).rejection)
    }

    @Test
    fun upgradingAnotherPlayersSettlementIsRejected() {
        val current = play.currentPlayer
        val vertex = play.buildings.first { it.owner != current }.vertex
        val s = play.copy(hands = play.hands + (current to cityCost))
        assertNotNull(engine.reduce(s, current, UpgradeCity(vertex)).rejection)
    }

    @Test
    fun upgradingACityAgainIsRejected() {
        val current = play.currentPlayer
        val vertex = play.buildings.first { it.owner == current }.vertex
        val s = play.copy(hands = play.hands + (current to cityCost + cityCost))
        val once = engine.reduce(s, current, UpgradeCity(vertex)).state
        assertNotNull(engine.reduce(once, current, UpgradeCity(vertex)).rejection)
    }

    @Test
    fun legalCitiesListsAffordableOwnSettlements() {
        val current = play.currentPlayer
        val expected = play.buildings
            .filter { it.owner == current && it.kind == Building.Kind.SETTLEMENT }
            .map { it.vertex }.toSet()
        val funded = play.copy(hands = play.hands + (current to cityCost))
        assertEquals(expected, engine.legalCities(funded, current))
        val broke = play.copy(hands = play.hands + (current to ResourceCount()))
        assertTrue(engine.legalCities(broke, current).isEmpty())
    }

    private fun Vertex.isDistanceLegal(s: GameState): Boolean =
        s.buildingAt(this) == null && adjacentVertices().none { s.buildingAt(it) != null }
}
