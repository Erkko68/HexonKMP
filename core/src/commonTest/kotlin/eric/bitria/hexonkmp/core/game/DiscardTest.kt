package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.DiscardResources
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The discard-half-on-7 step: players over 7 cards discard half before the
// robber moves; the phase only waits on present players.
class DiscardTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    private val brick = Resource.BRICK
    private val wool = Resource.WOOL

    @Test
    fun rollingASevenWithAnOversizedHandEntersDiscard() {
        val seed = generateSequence(0L) { it + 1 }.first { s ->
            val r = Random(s); r.nextInt(1, 7) + r.nextInt(1, 7) == 7
        }
        val s = play.copy(
            phase = GamePhase.Play,
            hands = play.hands + (alice to ResourceCount.of(brick to 10)),
            rngSeed = seed,
            currentPlayerIndex = play.players.indexOf(alice),
        )
        val result = engine.reduce(s, alice, EndTurn) // -> bob's turn begins, rolls 7
        assertEquals(7, result.state.lastRoll)
        val phase = result.state.phase
        assertTrue(phase is GamePhase.Discard)
        assertEquals(5, (phase as GamePhase.Discard).pending[alice]) // 10 / 2
    }

    @Test
    fun anOwerAbsentWhenTheSevenIsRolledIsAutoDiscardedNotWaitedOn() {
        val carol = PlayerId("carol")
        val threePlay = engine.completeSetup(engine.initialState(listOf(alice, bob, carol)))
        // Carol leaves, then holds too many cards when a 7 comes up.
        val withoutCarol = engine.playerLeft(threePlay, carol).state
        val seven = generateSequence(0L) { it + 1 }.first { s ->
            val r = Random(s); r.nextInt(1, 7) + r.nextInt(1, 7) == 7
        }
        val s = withoutCarol.copy(
            hands = withoutCarol.hands + (carol to ResourceCount.of(brick to 10)),
            rngSeed = seven,
            currentPlayerIndex = withoutCarol.players.indexOf(withoutCarol.currentPlayer),
        )
        // End the current present player's turn -> next present player rolls the 7.
        val result = engine.reduce(s, s.currentPlayer, EndTurn)
        assertEquals(7, result.state.lastRoll)
        // Carol was auto-discarded (10 -> 5 left); nobody else owes -> straight to robber.
        assertEquals(5, result.state.handOf(carol).total)
        assertEquals(GamePhase.Robber, result.state.phase)
    }

    @Test
    fun discardingTheExactCountClearsYourDebtThenMovesToRobber() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2)),
            hands = play.hands + (alice to ResourceCount.of(brick to 4)),
            currentPlayerIndex = play.players.indexOf(alice),
        )
        val result = engine.reduce(s, alice, DiscardResources(ResourceCount.of(brick to 2)))
        assertNull(result.rejection)
        assertEquals(2, result.state.handOf(alice)[brick])
        // Only ower, and the roller is present -> straight to the robber move.
        assertEquals(GamePhase.Robber, result.state.phase)
    }

    @Test
    fun discardingTheWrongCountIsRejected() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2)),
            hands = play.hands + (alice to ResourceCount.of(brick to 4)),
        )
        assertNotNull(engine.reduce(s, alice, DiscardResources(ResourceCount.of(brick to 1))).rejection)
    }

    @Test
    fun discardingCardsYouDontHaveIsRejected() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2)),
            hands = play.hands + (alice to ResourceCount.of(brick to 1, wool to 5)),
        )
        assertNotNull(engine.reduce(s, alice, DiscardResources(ResourceCount.of(brick to 2))).rejection)
    }

    @Test
    fun aPlayerWhoDoesntOweCannotDiscard() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2)),
            hands = play.hands + (bob to ResourceCount.of(brick to 4)),
        )
        assertNotNull(engine.reduce(s, bob, DiscardResources(ResourceCount.of(brick to 2))).rejection)
    }

    @Test
    fun discardWaitsForEveryPresentOwer() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2, bob to 2)),
            hands = play.hands + (alice to ResourceCount.of(brick to 4)) + (bob to ResourceCount.of(wool to 4)),
            currentPlayerIndex = play.players.indexOf(alice),
        )
        val afterAlice = engine.reduce(s, alice, DiscardResources(ResourceCount.of(brick to 2))).state
        assertEquals(GamePhase.Discard(mapOf(bob to 2)), afterAlice.phase) // still waiting on bob
        val afterBob = engine.reduce(afterAlice, bob, DiscardResources(ResourceCount.of(wool to 2))).state
        assertEquals(GamePhase.Robber, afterBob.phase)
    }

    @Test
    fun anAbsentOwerDoesNotBlockTheDiscardPhase() {
        // bob owes but is gone; alice discarding finishes the phase anyway.
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(alice to 2, bob to 2)),
            present = setOf(alice),
            hands = play.hands + (alice to ResourceCount.of(brick to 4)),
            currentPlayerIndex = play.players.indexOf(alice),
        )
        val result = engine.reduce(s, alice, DiscardResources(ResourceCount.of(brick to 2)))
        assertEquals(GamePhase.Robber, result.state.phase)
    }

    @Test
    fun aPlayerLeavingDuringDiscardAutoDiscardsAtRandomThenUnblocks() {
        val s = play.copy(
            phase = GamePhase.Discard(mapOf(bob to 2)),
            hands = play.hands + (bob to ResourceCount.of(wool to 4)),
            currentPlayerIndex = play.players.indexOf(alice),
        )
        val result = engine.playerLeft(s, bob)
        assertTrue(bob !in result.state.present)
        // Bob still pays the penalty: 2 of his 4 cards are discarded for him.
        assertEquals(2, result.state.handOf(bob).total)
        assertTrue(result.events.any { it is eric.bitria.hexonkmp.core.game.event.ResourcesDiscarded })
        assertEquals(GamePhase.Robber, result.state.phase) // alice (roller) present -> robber
    }
}
