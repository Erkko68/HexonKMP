package eric.bitria.hexon.game.vertex

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the Catan distance rule (no settlements adjacent to each other).
 * In Catan, settlements must be at least 2 edges apart.
 */
class DistanceRuleTest {

    @Test
    fun `cannot place village adjacent to existing village - shared edge h1-h2`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // Place first village at (h0, h1, h2)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // Try to place at neighbor via edge (h0, h1) - third hex is (1, -1)
        val neighborHex = HexCoord(1, -1)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h0, h1, neighborHex, TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Cannot place village adjacent to existing one (edge h0-h1)")
    }

    @Test
    fun `cannot place village adjacent to existing village - shared edge h1-h2 second neighbor`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // Neighbor via edge (h1, h2) - third hex is (1, 1)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h1, h2, HexCoord(1, 1), TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Cannot place village adjacent to existing one (edge h1-h2)")
    }

    @Test
    fun `cannot place village adjacent to existing village - shared edge h2-h0`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // Neighbor via edge (h2, h0) - third hex is (-1, 1)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h2, h0, HexCoord(-1, 1), TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Cannot place village adjacent to existing one (edge h2-h0)")
    }

    @Test
    fun `all three neighbor vertices are blocked`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // All 3 neighbors should be blocked
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h0, h1, HexCoord(1, -1),
            TestHelpers.VILLAGE, checkConnection = false))
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h1, h2, HexCoord(1, 1),
            TestHelpers.VILLAGE, checkConnection = false))
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h2, h0, HexCoord(-1, 1),
            TestHelpers.VILLAGE, checkConnection = false))
    }

    @Test
    fun `can place village two edges away from existing village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // Place first village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // Two edges away: (h1, 1,-1, 2,-1) - skips one vertex
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, HexCoord(1, -1), HexCoord(2, -1),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertTrue(canPlace, "Should be able to place village 2 edges away")
    }

    @Test
    fun `can place village on opposite side of same hex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Place village on one corner of center tile
        val center = HexCoord(0, 0)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE, TestHelpers.PLAYER_1,
            center, HexCoord(1, 0), HexCoord(0, 1),
            checkConnection = false
        )

        // Place on the opposite corner (3 edges away on the hex)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2,
            center, HexCoord(-1, 0), HexCoord(0, -1),
            TestHelpers.VILLAGE, checkConnection = false
        )
        assertTrue(canPlace, "Should be able to place village on opposite corner of same hex")
    }

    @Test
    fun `distance rule applies regardless of owner`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // Player 1 places a village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h0, h1, h2, checkConnection = false)

        // Player 2 cannot place adjacent (distance rule is universal)
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h0, h1, HexCoord(1, -1), TestHelpers.VILLAGE, checkConnection = false
        )
        assertFalse(canPlace, "Distance rule applies to all players")
    }

    @Test
    fun `multiple villages create larger blocked zone`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Place two villages at opposite corners of center hex (proper spacing)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE, TestHelpers.PLAYER_1,
            HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1),
            checkConnection = false
        )
        board.placeVertexBuilding(
            TestHelpers.VILLAGE, TestHelpers.PLAYER_2,
            HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1),
            checkConnection = false
        )

        // Player 1's village blocks its 3 adjacent vertices
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(1, -1),
            TestHelpers.VILLAGE, checkConnection = false
        ), "Should be blocked by Player 1's village")

        // Player 2's village blocks its 3 adjacent vertices
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(-1, 1),
            TestHelpers.VILLAGE, checkConnection = false
        ), "Should be blocked by Player 2's village")
    }
}

