package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceCountTest {

    @Test
    fun plusMergesAndSums() {
        val a = ResourceCount.of(Resource.BRICK to 1, Resource.ORE to 2)
        val b = ResourceCount.of(Resource.BRICK to 3)
        val sum = a + b
        assertEquals(4, sum[Resource.BRICK])
        assertEquals(2, sum[Resource.ORE])
        assertEquals(6, sum.total)
    }

    @Test
    fun minusDropsZeroEntries() {
        val a = ResourceCount.of(Resource.WOOL to 2)
        val result = a - ResourceCount.of(Resource.WOOL to 2)
        assertTrue(result.isEmpty)
        assertEquals(0, result[Resource.WOOL])
    }

    @Test
    fun coversChecksAffordability() {
        val hand = ResourceCount.of(Resource.BRICK to 1, Resource.LUMBER to 1)
        assertTrue(hand.covers(ResourceCount.of(Resource.BRICK to 1, Resource.LUMBER to 1)))
        assertFalse(hand.covers(ResourceCount.of(Resource.BRICK to 2)))
    }
}
