package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CatanGameEngineTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val state = engine.initialState(listOf(alice, bob))

    @Test
    fun firstPlayerStartsAndOwnsTurnOne() {
        assertEquals(alice, state.currentPlayer)
        assertEquals(1, state.turn)
    }

    @Test
    fun endTurnByCurrentPlayerAdvancesToNext() {
        val result = engine.reduce(state, alice, EndTurn)
        assertNull(result.rejection)
        assertEquals(bob, result.state.currentPlayer)
        // The first event is the turn change; an automatic DiceRolled follows.
        assertEquals(TurnChanged(bob, 1), result.events.first())
    }

    @Test
    fun wrappingBackToFirstPlayerIncrementsTurn() {
        val afterAlice = engine.reduce(state, alice, EndTurn).state
        val afterBob = engine.reduce(afterAlice, bob, EndTurn)
        assertEquals(alice, afterBob.state.currentPlayer)
        assertEquals(2, afterBob.state.turn)
    }

    @Test
    fun endTurnByWrongPlayerIsRejectedAndStateUnchanged() {
        val result = engine.reduce(state, bob, EndTurn)
        assertNotNull(result.rejection)
        assertEquals(state, result.state)
        assertEquals(emptyList(), result.events)
    }

    @Test
    fun currentPlayerLeavingPassesTurnToNextPresentPlayer() {
        // Alice is current; when she leaves, the turn must move to Bob so the
        // remaining players aren't stuck.
        val result = engine.playerLeft(state, alice)
        assertEquals(bob, result.state.currentPlayer)
        assertEquals(setOf(bob), result.state.present)
        assertEquals(TurnChanged(bob, 1), result.events.first())
    }

    @Test
    fun nonCurrentPlayerLeavingKeepsTurnButUpdatesPresence() {
        val result = engine.playerLeft(state, bob)
        assertEquals(alice, result.state.currentPlayer)
        assertEquals(setOf(alice), result.state.present)
        assertEquals(emptyList(), result.events)
    }

    @Test
    fun endTurnSkipsAbsentPlayers() {
        val carol = PlayerId("carol")
        val three = engine.initialState(listOf(alice, bob, carol))
        // Bob leaves (not current), then Alice ends turn -> should skip Bob to Carol.
        val afterBobLeft = engine.playerLeft(three, bob).state
        val afterEnd = engine.reduce(afterBobLeft, alice, EndTurn)
        assertEquals(carol, afterEnd.state.currentPlayer)
    }

    @Test
    fun rejoiningRestoresPresence() {
        val afterLeft = engine.playerLeft(state, bob).state
        val afterRejoin = engine.playerJoined(afterLeft, bob)
        assertEquals(setOf(alice, bob), afterRejoin.state.present)
    }
}
