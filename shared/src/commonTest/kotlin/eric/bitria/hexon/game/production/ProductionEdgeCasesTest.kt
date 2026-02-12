package eric.bitria.hexon.game.production

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Edge cases for resource production.
 */
class ProductionEdgeCasesTest {

    @Test
    fun `number 2 produces with probability - rare roll`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "ore", 2)
        // Move robber away from the tile
        board.moveRobber(HexCoord(10, 10))

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val production = board.getProductionForRoll(2)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("ore"))
    }

    @Test
    fun `number 12 produces with probability - rare roll`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wheat", 12)
        board.moveRobber(HexCoord(10, 10))

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val production = board.getProductionForRoll(12)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wheat"))
    }

    @Test
    fun `common numbers 6 and 8 produce correctly`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)
        board.moveRobber(HexCoord(10, 10))

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        var production = board.getProductionForRoll(6)
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wood"))

        production = board.getProductionForRoll(8)
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("brick"))
    }

    @Test
    fun `village on intersection of three tiles produces from all matching`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 5)
        board.addTile(HexCoord(1, 0), "brick", 5)
        board.addTile(HexCoord(0, 1), "sheep", 5)
        board.moveRobber(HexCoord(10, 10))

        // Vertex touching all three tiles
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val production = board.getProductionForRoll(5)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wood"))
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("brick"))
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("sheep"))
    }

    @Test
    fun `city on intersection of three tiles produces double from all matching`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "ore", 9)
        board.addTile(HexCoord(1, 0), "wheat", 9)
        board.addTile(HexCoord(0, 1), "sheep", 9)
        board.moveRobber(HexCoord(10, 10))

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1))

        val production = board.getProductionForRoll(9)

        assertEquals(2, production[TestHelpers.PLAYER_1]?.get("ore"))
        assertEquals(2, production[TestHelpers.PLAYER_1]?.get("wheat"))
        assertEquals(2, production[TestHelpers.PLAYER_1]?.get("sheep"))
    }

    @Test
    fun `mixed villages and cities produce correctly`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 4)
        board.moveRobber(HexCoord(10, 10))

        // Village on one corner
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // City on another corner (opposite side respects distance rule)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1))

        val production = board.getProductionForRoll(4)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wood"))
        assertEquals(2, production[TestHelpers.PLAYER_2]?.get("wood"))
    }

    @Test
    fun `same player with village and city on same tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "brick", 10)
        board.moveRobber(HexCoord(10, 10))

        // Village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // City on opposite corner
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1))

        val production = board.getProductionForRoll(10)

        assertEquals(3, production[TestHelpers.PLAYER_1]?.get("brick")) // 1 from village + 2 from city
    }

    @Test
    fun `coastal village produces from adjacent tiles only`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wheat", 6)
        board.moveRobber(HexCoord(10, 10))
        // Other two coordinates are water (no tiles)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val production = board.getProductionForRoll(6)

        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wheat"))
    }

    @Test
    fun `no crash when roll matches no tiles`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.moveRobber(HexCoord(10, 10))

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Roll a number that matches no tiles
        val production = board.getProductionForRoll(11)

        assertTrue(production.isEmpty() || production[TestHelpers.PLAYER_1]?.isEmpty() != false)
    }

    @Test
    fun `large production scenario - many buildings`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // Place buildings all over the board
        val placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)
        var placedCount = 0
        for (placement in placements.take(6)) {
            if (board.placeVertexBuilding(
                    TestHelpers.VILLAGE,
                    TestHelpers.PLAYER_1, placement.first, placement.second, placement.third, checkConnection = false)) {
                placedCount++
            }
        }

        assertTrue(placedCount > 0)

        // Roll each number and verify no crashes
        for (roll in 2..12) {
            if (roll != 7) {
                val production = board.getProductionForRoll(roll)
                // Just verify it doesn't crash
            }
        }
    }
}

