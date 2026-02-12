package eric.bitria.hexon.game.discovery

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for discovering available vertex placements (AI/UI helpers).
 */
class AvailableVertexPlacementsTest {

    @Test
    fun `returns empty list for empty board`() {
        val board = TestHelpers.createEmptyBoard()

        val placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)

        assertTrue(placements.isEmpty(), "Empty board should have no valid placements")
    }

    @Test
    fun `returns vertices for single tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)

        val placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)

        // A single hex has 6 corners
        assertEquals(6, placements.size, "Single tile should have 6 valid vertices")
    }

    @Test
    fun `adjacent tiles share vertices`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)

        val placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)

        // Two adjacent hexes share 2 vertices, so: 6 + 6 - 2 = 10
        assertEquals(10, placements.size, "Two adjacent tiles should have 10 unique vertices")
    }

    @Test
    fun `placed village blocks adjacent vertices`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        // Initial count
        val initialPlacements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)

        // Place a village
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val afterPlacements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)

        // Should block 1 (placed) + 3 (adjacent) = 4 vertices
        assertTrue(afterPlacements.size < initialPlacements.size - 1)
    }

    @Test
    fun `checkConnection filters based on road network`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        // No buildings - with connection check, no placements available
        val noConnectionPlacements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = true)
        assertTrue(noConnectionPlacements.isEmpty(), "No placements without road network")

        // Place initial village and road
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1))
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1))

        val withConnectionPlacements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = true)
        assertTrue(withConnectionPlacements.isNotEmpty(), "Should have placements along road network")
    }

    @Test
    fun `returns no duplicates`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        val placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)
        val uniqueIds = placements.map { (h1, h2, h3) -> HexCoord.getVertexId(h1, h2, h3) }.toSet()

        assertEquals(placements.size, uniqueIds.size, "Should not return duplicate vertices")
    }

    @Test
    fun `different players see same initial placements`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val p1Placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.VILLAGE, checkConnection = false)
        val p2Placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_2,
            TestHelpers.VILLAGE, checkConnection = false)

        assertEquals(p1Placements.size, p2Placements.size, "Both players should see same initial placements")
    }

    @Test
    fun `opponent's village blocks vertices for all players`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val p2Placements = board.getAvailableVertexPlacements(
            TestHelpers.PLAYER_2,
            TestHelpers.VILLAGE, checkConnection = false)
        val blockedVertex = Triple(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1))

        assertFalse(p2Placements.any { HexCoord.getVertexId(it.first, it.second, it.third) ==
            HexCoord.getVertexId(blockedVertex.first, blockedVertex.second, blockedVertex.third) })
    }
}

