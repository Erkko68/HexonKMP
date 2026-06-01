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

// Dice + production behavior. Dice only roll in the Play phase, so each test
// first drives setup to completion via completeSetup().
class DiceAndProductionTest {

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    @Test
    fun playBeginsWithAnAutomaticRollForTheFirstPlayer() {
        val engine = CatanGameEngine(boardSeed = 1)
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        assertNotNull(play.lastRoll)
        assertTrue(play.lastRoll!! in 2..12)
    }

    @Test
    fun endTurnRollsAutomaticallyForNextPlayer() {
        val engine = CatanGameEngine(boardSeed = 1)
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val result = engine.reduce(play, alice, EndTurn)

        assertEquals(bob, result.state.currentPlayer)
        // The roll happens as part of ending the turn — a DiceRolled event is emitted.
        val dice = result.events.filterIsInstance<DiceRolled>().single()
        assertEquals(dice.die1 + dice.die2, dice.total)
        assertEquals(dice.total, result.state.lastRoll)
    }

    @Test
    fun diceAreDeterministicForSameSeed() {
        fun firstRoll(seed: Long): Int {
            val engine = CatanGameEngine(boardSeed = seed)
            return engine.completeSetup(engine.initialState(listOf(alice, bob))).lastRoll!!
        }
        assertEquals(firstRoll(99), firstRoll(99))
    }

    @Test
    fun aSettlementCollectsResourceWhenItsTileTokenIsRolled() {
        // Complete setup, then add an extra settlement on a resource tile; over
        // many turns the tile's token comes up, and when it does Alice gains
        // exactly that tile's resource (and never on a 7).
        val engine = CatanGameEngine(boardSeed = 5)
        val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        val tile = play.board.tiles.first { it.terrain.resource != null }
        val resource = tile.terrain.resource!!

        var state = play.copy(
            buildings = play.buildings + Building(alice, cornerVertex(tile.hex, 0), Building.Kind.SETTLEMENT),
        )

        var sawProduction = false
        repeat(200) {
            if (state.currentPlayer != alice) {
                state = engine.reduce(state, state.currentPlayer, EndTurn).state
                return@repeat
            }
            val before = state.handOf(alice)[resource]
            val result = engine.reduce(state, alice, EndTurn)
            if (result.state.lastRoll == tile.token) {
                // Alice's building on this tile gained the resource on this roll.
                assertTrue(result.state.handOf(alice)[resource] >= before + 1)
                assertNotNull(result.events.filterIsInstance<ResourcesProduced>().firstOrNull())
                sawProduction = true
            }
            state = result.state
        }
        assertTrue(sawProduction, "expected the tile's token to come up at least once in 200 turns")
    }

    @Test
    fun rollOfSevenProducesNothing() {
        val engine = CatanGameEngine(boardSeed = 1)
        var state = engine.completeSetup(engine.initialState(listOf(alice, bob)))
        // Whenever a 7 comes up, no ResourcesProduced event is emitted.
        var sawSeven = false
        repeat(300) {
            val result = engine.reduce(state, state.currentPlayer, EndTurn)
            if (result.state.lastRoll == 7) {
                sawSeven = true
                assertTrue(result.events.none { it is ResourcesProduced })
            }
            state = result.state
        }
        assertTrue(sawSeven, "expected at least one 7 in 300 rolls")
    }
}
