package eric.bitria.hexon.game.vertex

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for basic village placement on vertices.
 */
class VillagePlacementTest {

    @Test
    fun `can place village on valid land vertex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Vertex at intersection of (0,0), (1,0), (0,1)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertTrue(canPlace, "Should be able to place village on valid land vertex")
    }

    @Test
    fun `placeVertexBuilding creates building at correct location`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        val success = board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        assertTrue(success)
        val vertexId = HexCoord.getVertexId(h1, h2, h3)
        assertNotNull(board.getBuildingAt(vertexId))
        assertEquals(TestHelpers.VILLAGE, board.getBuildingAt(vertexId)?.def?.id)
        assertEquals(TestHelpers.PLAYER_1, board.getBuildingAt(vertexId)?.ownerId)
    }

    @Test
    fun `cannot place village on water - no tiles exist`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // All coordinates are in the ocean (no tiles)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(10, 10), HexCoord(11, 10), HexCoord(10, 11),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Should not be able to place village in open water")
    }

    @Test
    fun `can place village on coastal vertex - at least one tile exists`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Only h1 (0,0) exists, h2 and h3 are water
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertTrue(canPlace, "Should be able to place village on coastal vertex")
    }

    @Test
    fun `cannot place village when coordinates are equal`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 0), HexCoord(1, 0),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Should not place village with duplicate coordinates")
    }

    @Test
    fun `cannot place village when all three coordinates are equal`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 0), HexCoord(0, 0),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Should not place village with all equal coordinates")
    }

    @Test
    fun `cannot place village on occupied vertex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        // Same player tries to place again
        val canPlace1 = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = false)
        assertFalse(canPlace1, "Should not place village on already occupied vertex (same player)")

        // Different player tries to place
        val canPlace2 = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = false)
        assertFalse(canPlace2, "Should not place village on already occupied vertex (different player)")
    }

    @Test
    fun `vertex id is order-independent`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        // Check with different orderings
        val id1 = HexCoord.getVertexId(h1, h2, h3)
        val id2 = HexCoord.getVertexId(h2, h1, h3)
        val id3 = HexCoord.getVertexId(h3, h2, h1)

        assertEquals(id1, id2)
        assertEquals(id2, id3)
        assertNotNull(board.getBuildingAt(id1))
        assertNotNull(board.getBuildingAt(id2))
        assertNotNull(board.getBuildingAt(id3))
    }
}

