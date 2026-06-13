package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.action.StealFrom
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The game-clock seam (timerKey / onTimeout) the server uses to run a clock without
// knowing Catan's rules. timerKey identifies the timed situation (a turn, a discard
// round); onTimeout returns the legal actions to auto-apply when it expires.
class TurnTimerTest {

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    private fun engine() = CatanGameEngine(boardSeed = 1)

    // The single action onTimeout would force (most phases auto-resolve one actor).
    private fun CatanGameEngine.singleTimeout(state: eric.bitria.hexonkmp.core.game.model.GameState) =
        onTimeout(state).single()

    @Test
    fun timerRunsForTheCurrentTurnAndStopsWhenFinished() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        assertTrue(engine.timerKey(play) != null)
        assertNull(engine.timerKey(play.copy(phase = GamePhase.Finished(alice))))
    }

    @Test
    fun aDiscardRoundGetsItsOwnClockSeparateFromTheTurn() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val discard = play.copy(phase = GamePhase.Discard(pending = mapOf(alice to 2)))
        // Distinct keys => entering a discard round resets the clock (a fresh global
        // countdown) rather than continuing the player's turn clock.
        assertTrue(engine.timerKey(play) != engine.timerKey(discard))
    }

    @Test
    fun timeoutInPlayEndsTheTurn() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val current = play.currentPlayer

        val timeout = engine.singleTimeout(play)
        assertEquals(current, timeout.actor)
        assertEquals(EndTurn, timeout.action)
        // Applying it actually hands the turn to the next player.
        assertTrue(engine.reduce(play, current, EndTurn).state.currentPlayer != current)
    }

    @Test
    fun timeoutDuringSetupPlacesALegalPiece() {
        val engine = engine()
        val start = engine.initialState(listOf(alice, bob))
        val timeout = engine.singleTimeout(start)
        assertTrue(timeout.action is PlaceSettlement) // setup opens awaiting a settlement
        assertNull(engine.reduce(start, timeout.actor, timeout.action).rejection)
    }

    @Test
    fun timeoutWhileOwingARobberMoveRelocatesTheRobber() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val robberPhase = play.copy(phase = GamePhase.Robber)

        val action = engine.singleTimeout(robberPhase).action
        assertTrue(action is MoveRobber)
        assertTrue(action.hex != robberPhase.board.robber, "must move to a different tile")
    }

    @Test
    fun timeoutWhileChoosingAStealTargetPicksAVictim() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val choose = play.copy(phase = GamePhase.ChooseStealTarget(listOf(bob)))

        assertEquals(StealFrom(bob), engine.singleTimeout(choose).action)
    }

    @Test
    fun timeoutWhilePlacingFreeRoadsPlacesARoad() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val roadBuilding = play.copy(phase = GamePhase.RoadBuilding(roadsLeft = 2))

        val timeout = engine.singleTimeout(roadBuilding)
        assertTrue(timeout.action is PlaceRoad) // setup gave them a road network
        assertNull(engine.reduce(roadBuilding, timeout.actor, timeout.action).rejection)
    }

    @Test
    fun discardTimeoutAutoDiscardsForEveryPresentOwer() {
        val engine = engine()
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        // Both players are over the limit and owe a discard.
        val discard = play.copy(
            phase = GamePhase.Discard(pending = mapOf(alice to 4, bob to 3)),
            hands = play.hands + mapOf(
                alice to ResourceCount.of(Resource.BRICK to 8),
                bob to ResourceCount.of(Resource.WOOL to 7),
            ),
        )

        val timeouts = engine.onTimeout(discard)
        assertEquals(setOf(alice, bob), timeouts.map { it.actor }.toSet())
        // Each forces a discard of exactly the owed count, and reduce accepts it.
        for (t in timeouts) {
            val action = t.action as DiscardResources
            val owed = discard.let { (it.phase as GamePhase.Discard).pending.getValue(t.actor) }
            assertEquals(owed, action.cards.total)
            assertNull(engine.reduce(discard, t.actor, action).rejection)
        }
    }
}
