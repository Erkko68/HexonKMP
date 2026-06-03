package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.event.ResourceStolen
import eric.bitria.hexonkmp.core.game.event.redactedFor
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The hidden-information transport seam: GameState.redactedFor and
// GameEvent.redactedFor must hide other players' secrets while preserving the
// public facts (counts).
class HiddenInfoTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carol = PlayerId("carol")

    private val state = engine.initialState(listOf(alice, bob)).copy(
        hands = mapOf(
            alice to ResourceCount.of(Resource.BRICK to 2),
            bob to ResourceCount.of(Resource.ORE to 3, Resource.WOOL to 1),
        ),
        devCards = mapOf(
            alice to listOf(DevCard.KNIGHT),
            bob to listOf(DevCard.MONOPOLY, DevCard.VICTORY_POINT),
        ),
        boughtThisTurn = mapOf(bob to listOf(DevCard.KNIGHT)),
    )

    @Test
    fun redactedStateHidesOtherPlayersHandsAndDevCards() {
        val forAlice = state.redactedFor(alice)
        // Alice keeps full visibility of her own cards...
        assertEquals(listOf(DevCard.KNIGHT), forAlice.devCards[alice])
        assertEquals(ResourceCount.of(Resource.BRICK to 2), forAlice.hands[alice])
        // ...but Bob's exact cards are stripped entirely.
        assertNull(forAlice.devCards[bob])
        assertNull(forAlice.hands[bob])
        assertNull(forAlice.boughtThisTurn[bob])
        // The deck's order/contents are hidden, only its size remains.
        assertTrue(forAlice.devDeck.isEmpty())
        assertEquals(state.devDeck.size, forAlice.devDeckSize)
    }

    @Test
    fun redactedStateExposesPublicCounts() {
        val forAlice = state.redactedFor(alice)
        // Resource-card counts are public for everyone.
        assertEquals(2, forAlice.resourceCounts[alice])
        assertEquals(4, forAlice.resourceCounts[bob])
        // Dev-card counts include bought-this-turn cards (Bob: 2 playable + 1 bought).
        assertEquals(1, forAlice.devCardCounts[alice])
        assertEquals(3, forAlice.devCardCounts[bob])
    }

    @Test
    fun stolenResourceTypeIsVisibleOnlyToThiefAndVictim() {
        val event = ResourceStolen(from = alice, by = bob, resource = Resource.WOOL)
        assertEquals(Resource.WOOL, (event.redactedFor(alice) as ResourceStolen).resource) // victim
        assertEquals(Resource.WOOL, (event.redactedFor(bob) as ResourceStolen).resource)   // thief
        assertNull((event.redactedFor(carol) as ResourceStolen).resource)                  // third party
    }

    @Test
    fun publicEventsPassThroughRedactionUnchanged() {
        val rolled = DiceRolled(3, 4, 7)
        assertEquals(rolled, rolled.redactedFor(carol))
    }
}
