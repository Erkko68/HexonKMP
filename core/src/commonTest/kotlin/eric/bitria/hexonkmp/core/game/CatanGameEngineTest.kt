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
        assertEquals(listOf(TurnChanged(bob, 1)), result.events)
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
}
