package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.GameEnded
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Turn-rotation / presence behavior, exercised in the Play phase (setup is
// driven to completion first via completeSetup()).
class CatanGameEngineTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carol = PlayerId("carol")

    // A game already past setup: snake draft completed, now in Play.
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
    // Three players, so one leaving still leaves a multi-player game running
    // (a leave that drops to a single survivor ends the game — see attrition tests).
    private val threePlay = engine.completeSetup(engine.initialState(listOf(alice, bob, carol)))

    @Test
    fun setupCompletesIntoPlayPhaseWithFirstPlayerCurrent() {
        assertEquals(GamePhase.Play, play.phase)
        assertEquals(alice, play.currentPlayer)
        assertEquals(1, play.turn)
    }

    @Test
    fun endTurnByCurrentPlayerAdvancesToNext() {
        val result = engine.reduce(play, alice, EndTurn)
        assertNull(result.rejection)
        assertEquals(bob, result.state.currentPlayer)
        // The first event is the turn change; an automatic DiceRolled follows.
        assertEquals(TurnChanged(bob, 1), result.events.first())
    }

    @Test
    fun wrappingBackToFirstPlayerIncrementsTurn() {
        val afterAlice = engine.reduce(play, alice, EndTurn).state
        val afterBob = engine.reduce(afterAlice, bob, EndTurn)
        assertEquals(alice, afterBob.state.currentPlayer)
        assertEquals(2, afterBob.state.turn)
    }

    @Test
    fun endTurnByWrongPlayerIsRejectedAndStateUnchanged() {
        val result = engine.reduce(play, bob, EndTurn)
        assertNotNull(result.rejection)
        assertEquals(play, result.state)
        assertEquals(emptyList(), result.events)
    }

    @Test
    fun currentPlayerLeavingPassesTurnToNextPresentPlayer() {
        // Alice is current; when she leaves, the turn must move to Bob so the
        // remaining players aren't stuck. (Three players, so a survivor remains.)
        val result = engine.playerLeft(threePlay, alice)
        assertEquals(bob, result.state.currentPlayer)
        assertEquals(setOf(bob, carol), result.state.present)
        assertEquals(TurnChanged(bob, 1), result.events.first())
    }

    @Test
    fun nonCurrentPlayerLeavingKeepsTurnButUpdatesPresence() {
        val result = engine.playerLeft(threePlay, bob)
        assertEquals(alice, result.state.currentPlayer)
        assertEquals(setOf(alice, carol), result.state.present)
        assertEquals(emptyList(), result.events)
    }

    @Test
    fun lastPlayerStandingWinsWhenEveryoneElseLeaves() {
        // Two-player game: when one leaves, the sole survivor is declared the winner
        // and the game ends immediately (no point advancing turns to nobody).
        val result = engine.playerLeft(play, alice)
        assertEquals(setOf(bob), result.state.present)
        assertEquals(GamePhase.Finished(bob), result.state.phase)
        assertEquals(listOf(GameEnded(bob)), result.events)
    }

    @Test
    fun endTurnSkipsAbsentPlayers() {
        val carol = PlayerId("carol")
        val threePlay = engine.completeSetup(engine.initialState(listOf(alice, bob, carol)))
        // Bob leaves (not current), then Alice ends turn -> should skip Bob to Carol.
        val afterBobLeft = engine.playerLeft(threePlay, bob).state
        val afterEnd = engine.reduce(afterBobLeft, alice, EndTurn)
        assertEquals(carol, afterEnd.state.currentPlayer)
    }

    @Test
    fun rejoiningRestoresPresence() {
        val afterLeft = engine.playerLeft(play, bob).state
        val afterRejoin = engine.playerJoined(afterLeft, bob)
        assertEquals(setOf(alice, bob), afterRejoin.state.present)
    }
}
