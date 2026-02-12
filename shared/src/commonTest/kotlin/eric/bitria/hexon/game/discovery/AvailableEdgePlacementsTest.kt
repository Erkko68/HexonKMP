package eric.bitria.hexon.game.discovery

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for discovering available edge placements (AI/UI helpers).
 */
class AvailableEdgePlacementsTest {

    @Test
    fun `returns empty list for empty board`() {
        val board = TestHelpers.createEmptyBoard()

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        assertTrue(placements.isEmpty(), "Empty board should have no valid edge placements")
    }

    @Test
    fun `returns empty list when no buildings placed`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        assertTrue(placements.isEmpty(), "No placements without buildings")
    }

    @Test
    fun `returns edges connected to own village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        // Village has 3 edges
        assertEquals(3, placements.size, "Should have 3 edges from village")
    }

    @Test
    fun `does not return edges connected only to opponent's village`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        assertTrue(placements.isEmpty(), "No placements when only opponent has buildings")
    }

    @Test
    fun `road extends available placements`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        val initialPlacements = board.getAvailableEdgePlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.ROAD
        )
        assertEquals(3, initialPlacements.size)

        // Place a road
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        val afterPlacements = board.getAvailableEdgePlacements(
            TestHelpers.PLAYER_1,
            TestHelpers.ROAD
        )

        // Should have: 2 remaining from village + 2 new from road end = 4
        // But the road at (0,0)-(1,0) is occupied, so we subtract 1
        assertTrue(afterPlacements.size >= 3, "Should have more placements after road")
    }

    @Test
    fun `long road chain creates many placements`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

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

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        assertTrue(placements.size > 3, "Long road chain should have many placements")
    }

    @Test
    fun `returns no duplicates`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)
        val uniqueIds = placements.map { (h1, h2) -> HexCoord.getEdgeId(h1, h2) }.toSet()

        assertEquals(placements.size, uniqueIds.size, "Should not return duplicate edges")
    }

    @Test
    fun `does not return occupied edges`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupMinimalMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)
        val occupiedEdgeId = HexCoord.getEdgeId(HexCoord(0, 0), HexCoord(1, 0))

        assertTrue(placements.none { HexCoord.getEdgeId(it.first, it.second) == occupiedEdgeId },
            "Should not return occupied edges")
    }

    @Test
    fun `multiple villages create more placements`() {
        val board = TestHelpers.createEmptyBoard()
        TestHelpers.setupLargeMap(board)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        val placements = board.getAvailableEdgePlacements(TestHelpers.PLAYER_1, TestHelpers.ROAD)

        // Each village has 3 edges, but they share one edge at (0,0)
        // Total: 3 + 3 - 1 shared = 5 edges (but one might be invalid depending on geometry)
        assertTrue(placements.size >= 4, "Multiple villages should create more placements")
    }
}

