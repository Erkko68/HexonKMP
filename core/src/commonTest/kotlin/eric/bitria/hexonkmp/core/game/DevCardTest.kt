package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.BuyDevCard
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlayKnight
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.DevCardBought
import eric.bitria.hexonkmp.core.game.event.DevCardPlayed
import eric.bitria.hexonkmp.core.game.event.GameEnded
import eric.bitria.hexonkmp.core.game.event.LargestArmyChanged
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Dev-card vertical slice: Buy + Knight + Largest Army. Starts each test from a
// played-out game (Play phase, alice current) and seeds dev state directly.
class DevCardTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    // The DEV_CARD price, handed to alice so she can afford one purchase.
    private val devCost = ResourceCount.of(Resource.ORE to 1, Resource.WOOL to 1, Resource.GRAIN to 1)

    private fun GameState.withAliceAbleToBuy(deck: List<DevCard>) =
        copy(devDeck = deck, hands = hands + (alice to devCost))

    @Test
    fun buyingDrawsTopCardIntoBoughtThisTurnAndSpends() {
        val state = play.withAliceAbleToBuy(listOf(DevCard.KNIGHT, DevCard.MONOPOLY))
        val result = engine.reduce(state, alice, BuyDevCard)

        assertEquals(listOf(DevCard.KNIGHT), result.state.boughtThisTurn[alice])
        assertEquals(1, result.state.devDeck.size)            // one drawn
        assertEquals(0, result.state.handOf(alice).total)     // cost spent
        val bought = result.events.filterIsInstance<DevCardBought>().single()
        assertEquals(DevCard.KNIGHT, bought.card)
        assertEquals(1, bought.deckSize)
    }

    @Test
    fun cannotBuyWithAnEmptyDeck() {
        val state = play.copy(devDeck = emptyList(), hands = play.hands + (alice to devCost))
        assertNotNull(engine.reduce(state, alice, BuyDevCard).rejection)
    }

    @Test
    fun cannotPlayACardBoughtThisTurn() {
        val state = play.withAliceAbleToBuy(listOf(DevCard.KNIGHT))
        val afterBuy = engine.reduce(state, alice, BuyDevCard).state
        // The knight is in boughtThisTurn, not yet playable.
        assertNotNull(engine.reduce(afterBuy, alice, PlayKnight).rejection)
    }

    @Test
    fun boughtCardsMatureIntoPlayableOnTheOwnersNextTurn() {
        val state = play.withAliceAbleToBuy(listOf(DevCard.KNIGHT))
        var s = engine.reduce(state, alice, BuyDevCard).state
        // Cycle back to alice's turn (resolve any robber a 7 may trigger).
        s = endTurnPastRobber(s, alice) // alice -> bob
        s = endTurnPastRobber(s, bob)   // bob -> alice (beginTurn matures her card)
        assertEquals(alice, s.currentPlayer)
        assertEquals(listOf(DevCard.KNIGHT), s.devCards[alice])
        assertTrue(s.boughtThisTurn[alice].isNullOrEmpty())
    }

    @Test
    fun playingAKnightEntersRobberAndCountsTheKnight() {
        val state = play.copy(devCards = mapOf(alice to listOf(DevCard.KNIGHT)))
        val result = engine.reduce(state, alice, PlayKnight)

        assertEquals(GamePhase.Robber, result.state.phase)
        assertEquals(1, result.state.knightsPlayed[alice])
        assertTrue(result.state.devCardPlayed)
        assertTrue(result.state.devCards[alice].isNullOrEmpty()) // knight consumed
        assertTrue(result.events.any { it is DevCardPlayed && it.card == DevCard.KNIGHT })
    }

    @Test
    fun thirdKnightAwardsLargestArmy() {
        val state = play.copy(
            devCards = mapOf(alice to listOf(DevCard.KNIGHT)),
            knightsPlayed = mapOf(alice to 2),
        )
        val result = engine.reduce(state, alice, PlayKnight)
        assertEquals(alice, result.state.largestArmy)
        assertTrue(result.events.any { it is LargestArmyChanged && it.holder == alice })
    }

    @Test
    fun onlyOneDevCardPerTurn() {
        val state = play.copy(devCards = mapOf(alice to listOf(DevCard.KNIGHT, DevCard.KNIGHT)))
        val afterKnight = engine.reduce(state, alice, PlayKnight).state
        // Finish the robber move, returning to Play with devCardPlayed still set.
        val backToPlay = engine.reduce(afterKnight, alice, MoveRobber(otherTile(afterKnight))).state
        assertEquals(GamePhase.Play, backToPlay.phase)
        assertNotNull(engine.reduce(backToPlay, alice, PlayKnight).rejection)
    }

    @Test
    fun victoryPointCardCanWinOnPurchase() {
        // Alice sits on 9 VP of buildings; buying a hidden VP card reaches 10.
        val vertices = play.board.vertices().toList()
        val nineVp = List(4) { Building(alice, vertices[it], Building.Kind.CITY) } +    // 8
            Building(alice, vertices[4], Building.Kind.SETTLEMENT)                      // +1
        val state = play.copy(buildings = nineVp)
            .withAliceAbleToBuy(listOf(DevCard.VICTORY_POINT))
        val result = engine.reduce(state, alice, BuyDevCard)
        assertEquals(GamePhase.Finished(alice), result.state.phase)
        assertTrue(result.events.any { it is GameEnded && it.winner == alice })
    }

    // --- helpers ---

    // Ends [who]'s turn; if their roll triggered the Robber phase, completes it.
    private fun endTurnPastRobber(state: GameState, who: PlayerId): GameState {
        var s = engine.reduce(state, who, EndTurn).state
        if (s.phase == GamePhase.Robber) {
            s = engine.reduce(s, s.currentPlayer, MoveRobber(otherTile(s))).state
        }
        return s
    }

    private fun otherTile(state: GameState) =
        state.board.tiles.map { it.hex }.first { it != state.board.robber }
}
