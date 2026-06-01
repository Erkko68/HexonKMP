package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.adjacentVertices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupPhaseTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val start = engine.initialState(listOf(alice, bob))

    @Test
    fun gameStartsInSetupAwaitingFirstSettlementWithNoRoll() {
        val phase = start.phase
        assertTrue(phase is GamePhase.Setup)
        assertEquals(alice, start.currentPlayer)
        assertNull(start.lastRoll) // no dice during setup
    }

    @Test
    fun snakeDraftOrderIsForwardThenReverse() {
        // 2 players -> 0,1,1,0: alice, bob, bob, alice.
        val seq = mutableListOf<PlayerId>()
        var state = start
        var guard = 0
        while (state.phase is GamePhase.Setup && guard++ < 100) {
            val p = state.currentPlayer
            seq += p
            val v = engine.legalSettlements(state, p).first()
            state = engine.reduce(state, p, PlaceSettlement(v)).state
            val e = engine.legalRoads(state, p).first()
            state = engine.reduce(state, p, PlaceRoad(e)).state
        }
        assertEquals(listOf(alice, bob, bob, alice), seq)
    }

    @Test
    fun cannotEndTurnDuringSetup() {
        val result = engine.reduce(start, alice, EndTurn)
        assertNotNull(result.rejection)
    }

    @Test
    fun mustPlaceSettlementBeforeRoad() {
        val anyEdge = start.board.edges().first()
        val result = engine.reduce(start, alice, PlaceRoad(anyEdge))
        assertNotNull(result.rejection)
    }

    @Test
    fun settlementDistanceRuleIsEnforced() {
        val v = engine.legalSettlements(start, alice).first()
        val afterSettlement = engine.reduce(start, alice, PlaceSettlement(v)).state
        // An adjacent vertex must now be illegal for the next settlement... but
        // first finish Alice's road and reach Bob, then check Bob can't build
        // adjacent to Alice's settlement.
        val edge = engine.legalRoads(afterSettlement, alice).first()
        val bobTurn = engine.reduce(afterSettlement, alice, PlaceRoad(edge)).state

        val neighbor = v.adjacentVertices().first()
        val rejected = engine.reduce(bobTurn, bob, PlaceSettlement(neighbor))
        assertNotNull(rejected.rejection)
    }

    @Test
    fun roadMustConnectToTheJustPlacedSettlement() {
        val v = engine.legalSettlements(start, alice).first()
        val afterSettlement = engine.reduce(start, alice, PlaceSettlement(v)).state
        // An edge not touching v should be rejected.
        val disconnected = afterSettlement.board.edges()
            .first { e -> v.hexes.none { it in e.hexes } }
        val rejected = engine.reduce(afterSettlement, alice, PlaceRoad(disconnected))
        assertNotNull(rejected.rejection)
    }

    @Test
    fun secondRoundSettlementGrantsStartingResources() {
        // Drive the snake to the second round (index >= order.size/2) and verify
        // the placing player receives resources from the settlement's tiles.
        var state = start
        // Round 1: alice, bob (no resources granted).
        repeat(2) {
            val p = state.currentPlayer
            val v = engine.legalSettlements(state, p).first()
            state = engine.reduce(state, p, PlaceSettlement(v)).state
            val e = engine.legalRoads(state, p).first()
            state = engine.reduce(state, p, PlaceRoad(e)).state
        }
        // Now in round 2 (bob first). Bob places his second settlement.
        val setup = state.phase as GamePhase.Setup
        assertTrue(setup.isSecondRound)
        val bobNow = state.currentPlayer
        val v2 = engine.legalSettlements(state, bobNow).first()
        val result = engine.reduce(state, bobNow, PlaceSettlement(v2))
        // Bob should have gained the resources of v2's resource-bearing tiles.
        assertTrue(result.state.handOf(bobNow).total > 0)
    }

    @Test
    fun setupCompletesIntoPlayAndRollsOnce() {
        val play = engine.completeSetup(start)
        assertEquals(GamePhase.Play, play.phase)
        assertNotNull(play.lastRoll)
        // Everyone placed two settlements + two roads.
        assertEquals(4, play.buildings.size)
        assertEquals(4, play.roads.size)
    }
}
