package eric.bitria.hexon.game.edge

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for road chaining - extending roads from existing roads.
 */
class RoadChainingTest {

    @Test
    fun `can chain road from existing road`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Setup: Village and first road
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        // Chain second road from first
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1),
            TestHelpers.ROAD
        )
        assertTrue(canPlace, "Should chain road from existing road")
    }

    @Test
    fun `can build long road chain`() {
        val board = TestHelpers.createBoard() // Use full board with all tiles

        // Initial setup - use vertices that exist on initialized board
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Build a chain of roads - staying within radius 2 board
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
    }

    @Test
    fun `cannot chain road from opponent's road`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Player 2 places village and road
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0))

        // Player 1 tries to chain from Player 2's road
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Should not chain road from opponent's road")
    }

    @Test
    fun `can chain in multiple directions from same vertex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Setup: Village at center
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Place roads in all 3 directions from village vertex
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(0, 1)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(0, 0)))
    }

    @Test
    fun `road can branch at vertex without building`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Initial setup
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1))

        // At vertex (1,0)-(1,-1)-(0,0) there's no building, but we can still branch
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1),
            TestHelpers.ROAD
        )
        assertTrue(canPlace, "Should branch road at empty vertex")
    }

    @Test
    fun `cannot skip vertex - must be adjacent`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        // Try to place road that doesn't connect (2,-1)-(2,0) skips (1,-1) vertex
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(2, -1), HexCoord(2, 0),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Should not skip vertices in road placement")
    }

    @Test
    fun `road blocked by opponent's village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 village and roads - start from far edge
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-2, 0), HexCoord(-1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-1, 0), HexCoord(0, 0))

        // Player 2 places village at the junction (respects distance rule)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Player 1 cannot continue road through Player 2's village
        val canPlace = board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0),
            TestHelpers.ROAD
        )
        assertFalse(canPlace, "Road should be blocked by opponent's village at vertex")
    }

    @Test
    fun `can build circular road network`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Build roads in a hexagonal pattern around center
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Build 6 roads around the center hex
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 1)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 1)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, -1)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, -1)))
    }
}

