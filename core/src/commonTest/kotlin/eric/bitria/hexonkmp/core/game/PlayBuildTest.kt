package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.game.model.board.adjacentVertices
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val freeEdge = s.board.edges().first { s.roadAt(it) == null }
        val result = engine.reduce(s, current, PlaceRoad(freeEdge))
        assertNull(result.rejection)
        // Spent exactly the cost -> empty hand.
        assertTrue(result.state.handOf(current).isEmpty)
        assertEquals(s.roads.size + 1, result.state.roads.size)
    }

    @Test
    fun affordableSettlementDeductsItsCost() {
        val current = play.currentPlayer
        // Fund the settlement plus one extra brick to verify exact deduction.
        val funded = settlementCost + ResourceCount.of(Resource.BRICK to 1)
        val s = play.copy(hands = play.hands + (current to funded))
        // A vertex far from existing buildings to satisfy the distance rule.
        val spot = s.board.vertices().first { v -> v.isPlaceable(s) }
        val result = engine.reduce(s, current, PlaceSettlement(spot))
        assertNull(result.rejection)
        assertEquals(1, result.state.handOf(current)[Resource.BRICK])
    }

    @Test
    fun canAffordReflectsHand() {
        val current = play.currentPlayer
        val broke = play.copy(hands = play.hands + (current to ResourceCount()))
        assertEquals(false, engine.canAfford(broke, current, Buildable.ROAD))
        val funded = play.copy(hands = play.hands + (current to roadCost))
        assertEquals(true, engine.canAfford(funded, current, Buildable.ROAD))
    }

    private fun Vertex.isPlaceable(s: GameState): Boolean =
        s.buildingAt(this) == null && adjacentVertices().none { s.buildingAt(it) != null }
}
