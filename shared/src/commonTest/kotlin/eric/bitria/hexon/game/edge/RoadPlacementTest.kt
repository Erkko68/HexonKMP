package eric.bitria.hexon.game.edge

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for basic road placement on edges.
 */
class RoadPlacementTest {

    @Test
    fun `can place road connected to own village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        val canPlace = board.canPlaceEdgeBuilding(TestHelpers.PLAYER_1, h1, h2, TestHelpers.ROAD)
        assertTrue(canPlace, "Should place road connected to own village")
    }

    @Test
    fun `road placement creates building at correct location`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)
        val success = board.placeEdgeBuilding(TestHelpers.ROAD, TestHelpers.PLAYER_1, h1, h2)

        assertTrue(success)
        val edgeId = HexCoord.getEdgeId(h1, h2)
        assertNotNull(board.getBuildingAt(edgeId))
        assertEquals(TestHelpers.ROAD, board.getBuildingAt(edgeId)?.def?.id)
        assertEquals(TestHelpers.PLAYER_1, board.getBuildingAt(edgeId)?.ownerId)
    }

    @Test
    fun `cannot place road connected to opponent's village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, h1, h2, h3, checkConnection = false)

        val canPlace = board.canPlaceEdgeBuilding(TestHelpers.PLAYER_1, h1, h2, TestHelpers.ROAD)
        assertFalse(canPlace, "Should not place road connected only to opponent's village")
    }

    @Test
    fun `cannot place floating road - no connection`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // No buildings placed, try to place road
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Should not place floating road with no connection")
    }

    @Test
    fun `cannot place road on water - no tiles`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Place a village first
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Try to place road in open water
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(50, 50), HexCoord(50, 51),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Should not place road in open water")
    }

    @Test
    fun `cannot place road on already occupied edge`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)
        board.placeEdgeBuilding(TestHelpers.ROAD, TestHelpers.PLAYER_1, h1, h2)

        // Same player tries again
        val canPlace1 = board.canPlaceEdgeBuilding(TestHelpers.PLAYER_1, h1, h2, TestHelpers.ROAD)
        assertFalse(canPlace1, "Should not place road on occupied edge (same player)")

        // Different player tries
        val canPlace2 = board.canPlaceEdgeBuilding(TestHelpers.PLAYER_2, h1, h2, TestHelpers.ROAD)
        assertFalse(canPlace2, "Should not place road on occupied edge (different player)")
    }

    @Test
    fun `cannot place road with equal coordinates`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 0),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Should not place road with equal coordinates")
    }

    @Test
    fun `edge id is order-independent`() {
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)

        val id1 = HexCoord.getEdgeId(h1, h2)
        val id2 = HexCoord.getEdgeId(h2, h1)

        assertEquals(id1, id2, "Edge ID should be order-independent")
    }

    @Test
    fun `can place road on coastal edge`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Place village on coast
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        // Place road on coastal edge (one hex exists)
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0),
            TestHelpers.ROAD
        )
        assertTrue(canPlace, "Should place road on coastal edge")
    }
}

