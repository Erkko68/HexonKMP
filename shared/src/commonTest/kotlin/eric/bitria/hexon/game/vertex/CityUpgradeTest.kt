package eric.bitria.hexon.game.vertex

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for upgrading villages to cities.
 */
class CityUpgradeTest {

    @Test
    fun `can upgrade own village to city`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.CITY
        )
        assertTrue(canPlace, "Should be able to upgrade own village to city")
    }

    @Test
    fun `upgrade changes building type to city`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)
        val success = board.placeVertexBuilding(TestHelpers.CITY, TestHelpers.PLAYER_1, h1, h2, h3)

        assertTrue(success)
        val vertexId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals(TestHelpers.CITY, board.getBuildingAt(vertexId)?.def?.id)
        assertEquals(TestHelpers.PLAYER_1, board.getBuildingAt(vertexId)?.ownerId)
    }

    @Test
    fun `cannot upgrade opponent's village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_2, h1, h2, h3,
            TestHelpers.CITY
        )
        assertFalse(canPlace, "Should not be able to upgrade opponent's village")
    }

    @Test
    fun `cannot place city on empty vertex`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.CITY, checkConnection = false)

        assertFalse(canPlace, "Should not be able to place city on empty vertex")
    }

    @Test
    fun `cannot upgrade city further`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)
        board.placeVertexBuilding(TestHelpers.CITY, TestHelpers.PLAYER_1, h1, h2, h3)

        // Try to upgrade city again
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.CITY
        )
        assertFalse(canPlace, "Should not be able to upgrade city further")
    }

    @Test
    fun `cannot downgrade city to village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, h1, h2, h3, checkConnection = false)
        board.placeVertexBuilding(TestHelpers.CITY, TestHelpers.PLAYER_1, h1, h2, h3)

        // Try to place village on city
        val canPlace = board.canPlaceVertexBuilding(
            TestHelpers.PLAYER_1, h1, h2, h3,
            TestHelpers.VILLAGE
        )
        assertFalse(canPlace, "Should not be able to downgrade city to village")
    }

    @Test
    fun `multiple players can have cities`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Player 1 city
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1))

        // Player 2 city (different location)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1))

        val city1 = board.getBuildingAt(HexCoord.getVertexId(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1)))
        val city2 = board.getBuildingAt(HexCoord.getVertexId(HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1)))

        assertEquals(TestHelpers.CITY, city1?.def?.id)
        assertEquals(TestHelpers.PLAYER_1, city1?.ownerId)
        assertEquals(TestHelpers.CITY, city2?.def?.id)
        assertEquals(TestHelpers.PLAYER_2, city2?.ownerId)
    }

    @Test
    fun `city preserves owner after upgrade`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, h1, h2, h3, checkConnection = false)
        board.placeVertexBuilding(TestHelpers.CITY, TestHelpers.PLAYER_3, h1, h2, h3)

        val vertexId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals(
            TestHelpers.PLAYER_3,
            board.getBuildingAt(vertexId)?.ownerId,
            "Owner should be preserved after upgrade"
        )
    }
}

