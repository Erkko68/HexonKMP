package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankTradeTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
    private val ratio = play.config.rules.bankTradeRatio

    @Test
    fun tradeSwapsAtTheBankRatio() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio)))
        val result = engine.reduce(s, current, BankTrade(listOf(BankSwap(Resource.ORE, Resource.BRICK))))
        assertNull(result.rejection)
        val hand = result.state.handOf(current)
        assertEquals(0, hand[Resource.ORE])     // gave away `ratio` ore
        assertEquals(1, hand[Resource.BRICK])   // received 1 brick
        assertEquals(
            BankTraded(current, ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 1)),
            result.events.single(),
        )
    }

    @Test
    fun multipleSwapsApplyAtomically() {
        // 2*ratio ore -> 1 brick + 1 wheat in one trade.
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio * 2)))
        val result = engine.reduce(
            s, current,
            BankTrade(listOf(BankSwap(Resource.ORE, Resource.BRICK), BankSwap(Resource.ORE, Resource.GRAIN))),
        )
        assertNull(result.rejection)
        val hand = result.state.handOf(current)
        assertEquals(0, hand[Resource.ORE])
        assertEquals(1, hand[Resource.BRICK])
        assertEquals(1, hand[Resource.GRAIN])
    }

    @Test
    fun partialFundsRejectTheWholeTrade() {
        // Enough for one swap, not two -> the atomic trade is rejected entirely.
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio)))
        val result = engine.reduce(
            s, current,
            BankTrade(listOf(BankSwap(Resource.ORE, Resource.BRICK), BankSwap(Resource.ORE, Resource.GRAIN))),
        )
        assertNotNull(result.rejection)
        assertEquals(ratio, result.state.handOf(current)[Resource.ORE]) // unchanged
    }

    @Test
    fun tradeWithoutEnoughResourcesIsRejected() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio - 1)))
        val result = engine.reduce(s, current, BankTrade(listOf(BankSwap(Resource.ORE, Resource.BRICK))))
        assertNotNull(result.rejection)
    }

    @Test
    fun tradeForSameResourceIsRejected() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio)))
        val result = engine.reduce(s, current, BankTrade(listOf(BankSwap(Resource.ORE, Resource.ORE))))
        assertNotNull(result.rejection)
    }

    @Test
    fun onlyCurrentPlayerCanTrade() {
        val other = play.players.first { it != play.currentPlayer }
        val s = play.copy(hands = play.hands + (other to ResourceCount.of(Resource.ORE to ratio)))
        val result = engine.reduce(s, other, BankTrade(listOf(BankSwap(Resource.ORE, Resource.BRICK))))
        assertNotNull(result.rejection)
    }
}
