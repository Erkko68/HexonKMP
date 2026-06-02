package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.FinalizeTrade
import eric.bitria.hexonkmp.core.game.action.ProposeTrade
import eric.bitria.hexonkmp.core.game.action.RespondTrade
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.TradeFinalized
import eric.bitria.hexonkmp.core.game.event.TradeOffersCleared
import eric.bitria.hexonkmp.core.game.event.TradeResponded
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Player-to-player trades: propose (current player) -> respond (opponents) ->
// finalize (proposer with one accepter). The engine re-validates both hands at
// finalize and clears all offers afterward. Offers also clear on turn change.
class PlayerTradeTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    // Alice (the current player after setup) gives 2 brick, wants 1 grain.
    private val give = ResourceCount.of(Resource.BRICK to 2)
    private val receive = ResourceCount.of(Resource.GRAIN to 1)

    // Funds both hands exactly so the trade is affordable on both sides.
    private fun funded(): GameState = play.copy(
        hands = play.hands + (alice to give) + (bob to receive),
    )

    @Test
    fun proposeThenFinalizeMovesResourcesBothWays() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        s = engine.reduce(s, bob, RespondTrade(offerId, accept = true)).state

        val result = engine.reduce(s, alice, FinalizeTrade(offerId, bob))
        assertNull(result.rejection)
        val after = result.state
        // Alice gave brick, got grain; Bob the reverse.
        assertEquals(0, after.handOf(alice)[Resource.BRICK])
        assertEquals(1, after.handOf(alice)[Resource.GRAIN])
        assertEquals(2, after.handOf(bob)[Resource.BRICK])
        assertEquals(0, after.handOf(bob)[Resource.GRAIN])
        assertEquals(TradeFinalized(offerId, alice, bob, give, receive), result.events.single())
        assertTrue(after.pendingTrades.isEmpty())
    }

    @Test
    fun proposeRejectedWhenProposerCannotCoverGive() {
        val s = play.copy(hands = play.hands + (alice to ResourceCount.of(Resource.BRICK to 1)))
        val result = engine.reduce(s, alice, ProposeTrade(give, receive))
        assertNotNull(result.rejection)
        assertTrue(result.state.pendingTrades.isEmpty())
    }

    @Test
    fun proposeRejectedForEmptyOrOverlappingResources() {
        val s = funded()
        assertNotNull(engine.reduce(s, alice, ProposeTrade(ResourceCount(), receive)).rejection)
        assertNotNull(engine.reduce(s, alice, ProposeTrade(give, ResourceCount())).rejection)
        // give and receive share BRICK -> nonsensical.
        val overlap = ResourceCount.of(Resource.BRICK to 1)
        assertNotNull(engine.reduce(s, alice, ProposeTrade(give, overlap)).rejection)
    }

    @Test
    fun onlyCurrentPlayerCanPropose() {
        val s = play.copy(hands = play.hands + (bob to give))
        val result = engine.reduce(s, bob, ProposeTrade(give, receive))
        assertNotNull(result.rejection)
    }

    @Test
    fun opponentCanRespondDuringProposersTurn() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        // Bob is NOT the current player, yet may respond.
        val result = engine.reduce(s, bob, RespondTrade(offerId, accept = true))
        assertNull(result.rejection)
        assertEquals(TradeResponded(offerId, bob, true), result.events.single())
        assertTrue(bob in result.state.pendingTrades.single().accepters)
    }

    @Test
    fun acceptRejectedWhenResponderCannotCoverReceive() {
        var s = play.copy(hands = play.hands + (alice to give) + (bob to ResourceCount()))
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        val result = engine.reduce(s, bob, RespondTrade(offerId, accept = true))
        assertNotNull(result.rejection)
    }

    @Test
    fun proposerCannotRespondToOwnOffer() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        assertNotNull(engine.reduce(s, alice, RespondTrade(offerId, accept = true)).rejection)
    }

    @Test
    fun finalizeWithNonAccepterIsRejected() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        // Bob never accepted (or declined).
        val declined = engine.reduce(s, bob, RespondTrade(offerId, accept = false)).state
        assertNotNull(engine.reduce(declined, alice, FinalizeTrade(offerId, bob)).rejection)
    }

    @Test
    fun finalizeRevalidatesHandsAtFinalizeTime() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val offerId = s.pendingTrades.single().id
        s = engine.reduce(s, bob, RespondTrade(offerId, accept = true)).state

        // Partner no longer holds the receive resource -> rejected (anti-cheat).
        val bobBroke = s.copy(hands = s.hands + (bob to ResourceCount()))
        assertNotNull(engine.reduce(bobBroke, alice, FinalizeTrade(offerId, bob)).rejection)

        // Proposer no longer holds the give resource -> rejected.
        val aliceBroke = s.copy(hands = s.hands + (alice to ResourceCount()))
        assertNotNull(engine.reduce(aliceBroke, alice, FinalizeTrade(offerId, bob)).rejection)
    }

    @Test
    fun finalizeClearsAllPendingOffers() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        // A second, different offer is also live.
        s = engine.reduce(s, alice, ProposeTrade(give, ResourceCount.of(Resource.WOOL to 1))).state
        val firstId = s.pendingTrades.first().id
        s = engine.reduce(s, bob, RespondTrade(firstId, accept = true)).state
        assertEquals(2, s.pendingTrades.size)

        val after = engine.reduce(s, alice, FinalizeTrade(firstId, bob)).state
        assertTrue(after.pendingTrades.isEmpty())
    }

    @Test
    fun endingTurnClearsOffersAndEmitsEvent() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        assertEquals(1, s.pendingTrades.size)
        val result = engine.reduce(s, alice, EndTurn)
        assertTrue(result.state.pendingTrades.isEmpty())
        assertTrue(result.events.any { it is TradeOffersCleared })
    }

    @Test
    fun offerIdsStayUniqueAcrossClears() {
        var s = funded()
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        val firstId = s.pendingTrades.single().id
        s = engine.reduce(s, bob, RespondTrade(firstId, accept = true)).state
        s = engine.reduce(s, alice, FinalizeTrade(firstId, bob)).state // clears all
        // Re-fund and propose again within the same turn — id must not recycle.
        s = s.copy(hands = s.hands + (alice to give))
        s = engine.reduce(s, alice, ProposeTrade(give, receive)).state
        assertTrue(s.pendingTrades.single().id != firstId)
    }
}
