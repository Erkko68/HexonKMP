package eric.bitria.hexon.game.vertex

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for multi-player scenarios on the board.
 */
class MultiPlayerTest {

    @Test
    fun `four players can all place villages`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false))
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false))
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, HexCoord(2, 0), HexCoord(2, 1), HexCoord(1, 1), checkConnection = false))
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_4, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false))

        assertEquals(4, board.buildings.size)
    }

    @Test
    fun `players cannot build on each other's occupied vertices`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1),
            TestHelpers.VILLAGE, checkConnection = false))
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_3, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1),
            TestHelpers.VILLAGE, checkConnection = false))
        assertFalse(board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_4, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1),
            TestHelpers.VILLAGE, checkConnection = false))
    }

    @Test
    fun `players can share tile but not vertex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Players place on opposite corners of the center hex (respects distance rule)
        // Opposite corners are 3 edges apart on the same hex
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false))
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false))

        // Place other players on different tiles to avoid distance rule violations
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, HexCoord(2, 0), HexCoord(2, 1), HexCoord(1, 1), checkConnection = false))
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_4, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false))

        assertEquals(4, board.buildings.size)
    }

    @Test
    fun `road networks are player-specific`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 builds road network
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        // Player 2 cannot use Player 1's roads
        assertFalse(board.canPlaceEdgeBuilding(
            TestHelpers.PLAYER_2, HexCoord(1, 0), HexCoord(1, -1),
            TestHelpers.ROAD
        ))
    }

    @Test
    fun `players can have adjacent road networks`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1's network
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        // Player 2's network - on opposite side of board
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(-2, 0), HexCoord(-2, 1), HexCoord(-1, 0), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-2, 0), HexCoord(-1, 0))

        assertEquals(4, board.buildings.size)
    }

    @Test
    fun `each player tracks their own buildings`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        val p1Buildings = board.buildings.values.filter { it.ownerId == TestHelpers.PLAYER_1 }
        val p2Buildings = board.buildings.values.filter { it.ownerId == TestHelpers.PLAYER_2 }

        assertEquals(1, p1Buildings.size)
        assertEquals(1, p2Buildings.size)
    }

    @Test
    fun `production goes to correct players`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.moveRobber(HexCoord(10, 10)) // Move robber away

        // Place villages at opposite corners (respects distance rule)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        val production = board.getProductionForRoll(6)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wood"))
        assertEquals(1, production[TestHelpers.PLAYER_2]?.get("wood"))
    }

    @Test
    fun `robber affects all players on tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wheat", 8)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        board.moveRobber(HexCoord(0, 0))

        val production = board.getProductionForRoll(8)

        assertEquals(0, production[TestHelpers.PLAYER_1]?.get("wheat") ?: 0)
        assertEquals(0, production[TestHelpers.PLAYER_2]?.get("wheat") ?: 0)
    }
}

