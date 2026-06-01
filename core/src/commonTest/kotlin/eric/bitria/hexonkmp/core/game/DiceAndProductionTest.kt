package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.ResourcesProduced
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.cornerVertex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiceAndProductionTest {

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    @Test
    fun gameStartsWithAnAutomaticRollForTheFirstPlayer() {
        val state = CatanGameEngine(boardSeed = 1).initialState(listOf(alice, bob))
        assertNotNull(state.lastRoll)
        assertTrue(state.lastRoll!! in 2..12)
    }

    @Test
    fun endTurnRollsAutomaticallyForNextPlayer() {
        val engine = CatanGameEngine(boardSeed = 1)
        val state = engine.initialState(listOf(alice, bob))
        val result = engine.reduce(state, alice, EndTurn)

        assertEquals(bob, result.state.currentPlayer)
        // The roll happens as part of ending the turn — a DiceRolled event is emitted.
        val dice = result.events.filterIsInstance<DiceRolled>().single()
        assertEquals(dice.die1 + dice.die2, dice.total)
        assertEquals(dice.total, result.state.lastRoll)
    }

    @Test
    fun diceAreDeterministicForSameSeed() {
        fun firstRoll(seed: Long): Int =
            CatanGameEngine(boardSeed = seed).initialState(listOf(alice, bob)).lastRoll!!
        assertEquals(firstRoll(99), firstRoll(99))
    }

    @Test
    fun aSettlementCollectsResourceWhenItsTileTokenIsRolled() {
        // Drive a full game and place a settlement on a resource tile; over many
        // turns the tile's token will come up, and when it does Alice must gain
        // exactly that tile's resource (and never on a 7).
        val engine = CatanGameEngine(boardSeed = 5)
        val base = engine.initialState(listOf(alice, bob))
        val tile = base.board.tiles.first { it.terrain.resource != null }
        val resource = tile.terrain.resource!!

        var state = base.copy(
            buildings = listOf(Building(alice, cornerVertex(tile.hex, 0), Building.Kind.SETTLEMENT)),
        )

        var sawProduction = false
        repeat(200) {
            val before = state.handOf(alice)[resource]
            val result = engine.reduce(state, state.currentPlayer, EndTurn)
            val produced = result.events.filterIsInstance<ResourcesProduced>().firstOrNull()
            if (result.state.lastRoll == tile.token) {
                // Token matched -> Alice gained the tile's resource this roll.
                assertEquals(before + 1, result.state.handOf(alice)[resource])
                assertNotNull(produced)
                sawProduction = true
            }
            state = result.state
        }
        assertTrue(sawProduction, "expected the tile's token to come up at least once in 200 rolls")
    }

    @Test
    fun rollOfSevenProducesNothing() {
        val engine = CatanGameEngine(boardSeed = 1)
        var state = engine.initialState(listOf(alice, bob))
        val tile = state.board.tiles.first { it.terrain.resource != null }
        state = state.copy(
            buildings = listOf(Building(alice, cornerVertex(tile.hex, 0), Building.Kind.SETTLEMENT)),
        )
        // Whenever a 7 comes up, no ResourcesProduced event is emitted.
        var sawSeven = false
        repeat(200) {
            val result = engine.reduce(state, state.currentPlayer, EndTurn)
            if (result.state.lastRoll == 7) {
                sawSeven = true
                assertTrue(result.events.none { it is ResourcesProduced })
            }
            state = result.state
        }
        assertTrue(sawSeven, "expected at least one 7 in 200 rolls")
    }
}
