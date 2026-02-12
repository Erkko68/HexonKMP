package eric.bitria.hexon.game.vertex

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for road connection requirements when placing villages.
 * During normal gameplay (not setup), villages must connect to player's roads.
 */
class VillageConnectionTest {

    @Test
    fun `cannot place village without road connection during normal play`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // checkConnection = true (default), no roads placed
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = true)
        assertFalse(canPlace, "Should not place village without road connection")
    }

    @Test
    fun `can place village with direct road connection`() {
        val board = TestHelpers.createBoard()

        // Target vertex at (0,0), (1,0), (0,1)
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // Place village at vertex: (0,0), (-1,0), (0,-1)
        // This village has edges: (0,0)-(-1,0), (-1,0)-(0,-1), (0,-1)-(0,0)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1,0), HexCoord(0, -1), checkConnection = false)

        // Build road chain that properly connects vertices through shared vertices:
        // Village vertex: {(0,0), (-1,0), (0,-1)}
        //
        // Road 1: (0,0)-(0,-1) - on village edge, connects to vertex {(0,-1), (0,0), (1,-1)}
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, -1)), "Road 1")

        // Road 2: (0,0)-(1,-1) - shares vertex {(0,-1), (0,0), (1,-1)} with Road 1
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, -1)), "Road 2")

        // Road 3: (0,0)-(1,0) - shares vertex {(0,0), (1,-1), (1,0)} with Road 2
        // This edge also touches target vertex {(0,0), (1,0), (0,1)}
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)), "Road 3")

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = true)
        assertTrue(canPlace, "Should place village with road on edge (0,0)-(1,0)")
    }

    @Test
    fun `road on edge h2-h3 enables village placement`() {
        val board = TestHelpers.createBoard()

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // Village at vertex (0,0), (-1,0), (0,-1)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        // Build road chain to edge (1,0)-(0,1) which is on target vertex
        // Path through properly connected vertices:
        // Village vertex {(0,0), (-1,0), (0,-1)}
        // → edge (0,0)-(0,-1) → vertex {(0,0), (0,-1), (1,-1)}
        // → edge (1,-1)-(0,0) → vertex {(0,-1), (1,-1), (0,0)} shares with previous; also touches {(1,-1), (0,0), (1,0)}
        // → edge (0,0)-(1,0) → vertex {(1,-1), (0,0), (1,0)} shares with previous; also touches target {(0,0), (1,0), (0,1)}
        // → edge (1,0)-(0,1) → on target vertex {(0,0), (1,0), (0,1)}
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, -1)), "Road 1")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(0, 0)), "Road 2")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)), "Road 3")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(0, 1)), "Road 4")

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = true)
        assertTrue(canPlace, "Should place village with road on edge (1,0)-(0,1)")
    }

    @Test
    fun `road on edge h3-h1 enables village placement`() {
        val board = TestHelpers.createBoard()

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // Village at vertex (0,0), (-1,0), (0,-1)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        // Build road chain to edge (0,1)-(0,0) which is on target vertex
        // Path: Village vertex {(0,0), (-1,0), (0,-1)}
        // → edge (0,0)-(-1,0) → vertex {(0,0), (-1,0), (-1,1)}
        // → edge (-1,0)-(-1,1) → vertex {(-1,0), (-1,1), (0,0)}
        // → edge (-1,1)-(0,0) → vertex {(-1,1), (0,0), (0,1)}
        // → edge (0,0)-(0,1) → target vertex {(0,0), (1,0), (0,1)}
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0)), "Road 1")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-1, 0), HexCoord(-1, 1)), "Road 2")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-1, 1), HexCoord(0, 0)), "Road 3")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 1)), "Road 4")

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = true)
        assertTrue(canPlace, "Should place village with road on edge (0,1)-(0,0)")
    }

    @Test
    fun `opponent's road does not satisfy connection`() {
        val board = TestHelpers.createBoard()

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // Player 2 places village and road
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(1, 0), HexCoord(1, -1), HexCoord(2, -1), checkConnection = false)
        board.placeEdgeBuilding(TestHelpers.ROAD, TestHelpers.PLAYER_2, h1, h2)

        // Player 1 tries to place village using Player 2's road
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = true)
        assertFalse(canPlace, "Opponent's road should not satisfy connection requirement")
    }

    @Test
    fun `can place village during setup without road - checkConnection false`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE, checkConnection = false)
        assertTrue(canPlace, "Should place village during setup without road")
    }

    @Test
    fun `long road chain allows village at end`() {
        val board = TestHelpers.createBoard()

        // Place initial village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Build roads - each edge must share a vertex with the previous
        // From village at (0,0), (1,0), (0,1):
        //   Edge (0,0)-(1,0) is on this vertex
        //   Then (1,0)-(1,-1) shares vertex (0,0), (1,0), (1,-1) with previous
        //   Then (1,-1)-(2,-1) shares vertex (1,0), (1,-1), (2,-1) with previous
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1))

        // Can place village at vertex (1,-1), (2,-1), (2,-2) - which touches edge (1,-1)-(2,-1)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1), HexCoord(2, -2),
            TestHelpers.VILLAGE, checkConnection = true
        )
        assertTrue(canPlace, "Should place village at end of long road chain")
    }
}
