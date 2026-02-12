package eric.bitria.hexon.game.edge

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for road blocking scenarios - opponent's village interrupts road networks.
 */
class RoadBlockingTest {

    @Test
    fun `cannot chain road through opponent's village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 sets up village and road
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-1, 0), HexCoord(0, 0))

        // Player 2 places village at the junction vertex (0,0), (1,0), (0,1)
        // This is more than 2 edges from Player 1's village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Player 1 cannot chain through Player 2's village at (0,0)
        // They want to place road on edge (0,0)-(1,0) but Player 2 owns the vertex
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Road should be blocked by opponent's village")
    }

    @Test
    fun `own village does not block road chaining`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 builds village, road, then another village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1))
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1), HexCoord(2, -1), checkConnection = true)

        // Can still chain from own village
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1),
            TestHelpers.ROAD
        )
        assertTrue(canPlace, "Own village should not block road chaining")
    }

    @Test
    fun `blocked road still allows alternate paths`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 village at center
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Player 2 village far enough away (opposite side)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false)

        // Player 1 can build roads in available directions
        assertTrue(board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 1),
            TestHelpers.ROAD
        ))
        assertTrue(board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(1, 0),
            TestHelpers.ROAD
        ))
    }

    @Test
    fun `can build road on both sides of opponent's village`() {
        val board = TestHelpers.createBoard()

        // Player 1 has two villages with proper spacing
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(2, 0), HexCoord(2, 1), HexCoord(1, 1), checkConnection = false)

        // Player 2's village in the middle (separate from both Player 1 villages)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Player 1 can build roads from both villages
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-1, 0)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(2, 0), HexCoord(1, 1)))
    }

    @Test
    fun `road network can be built in sequence`() {
        val board = TestHelpers.createBoard()

        // Player 1 builds a line of roads - each edge must share a vertex with previous
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)), "Road 1")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1)), "Road 2")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1)), "Road 3")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(2, -1), HexCoord(2, -2)), "Road 4")

        // All 4 roads should be placed
        val roadCount = board.buildings.values.count { it.def.id == "road" }
        assertEquals(4, roadCount)
    }
}

