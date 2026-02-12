package eric.bitria.hexon.game.robber

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for robber mechanics - blocking production and returning affected players.
 */
class RobberTest {

    @Test
    fun `robber blocks production on its tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 9)
        board.addTile(HexCoord(1, 0), "brick", 9)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Move robber to wood tile
        board.moveRobber(HexCoord(0, 0))

        val production = board.getProductionForRoll(9)

        // Wood (robbed) should produce 0, brick should produce 1
        assertEquals(0, production[TestHelpers.PLAYER_1]?.get("wood") ?: 0, "Robbed tile should produce nothing")
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("brick"), "Unrobbed tile should produce normally")
    }

    @Test
    fun `robber only blocks one tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 5)
        board.addTile(HexCoord(1, 0), "wood", 5)
        board.addTile(HexCoord(2, 0), "wood", 5)

        // Villages on each tile
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(0, 1), HexCoord(-1, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, 1), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(2, 0), HexCoord(2, 1), HexCoord(1, 1), checkConnection = false)

        // Rob middle tile only
        board.moveRobber(HexCoord(1, 0))

        val production = board.getProductionForRoll(5)

        // 2 tiles produce (0,0 and 2,0), middle is blocked
        assertEquals(2, production[TestHelpers.PLAYER_1]?.get("wood"), "Two unrobbed tiles should produce 2 wood")
    }

    @Test
    fun `moveRobber returns affected players`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)

        // 3 players around the hex
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 1), HexCoord(-1, 0), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, HexCoord(0, 0), HexCoord(0, -1), HexCoord(1, -1), checkConnection = false)

        val victims = board.moveRobber(HexCoord(0, 0))

        assertEquals(3, victims.size, "Should return 3 affected players")
        assertTrue(victims.contains(TestHelpers.PLAYER_1))
        assertTrue(victims.contains(TestHelpers.PLAYER_2))
        assertTrue(victims.contains(TestHelpers.PLAYER_3))
    }

    @Test
    fun `moveRobber returns unique players only`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wheat", 4)

        // Same player on multiple vertices of the same hex
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        val victims = board.moveRobber(HexCoord(0, 0))

        assertEquals(1, victims.size, "Should return player only once")
        assertEquals(TestHelpers.PLAYER_1, victims.first())
    }

    @Test
    fun `moveRobber returns empty list when no buildings on tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "ore", 3)
        // No buildings placed

        val victims = board.moveRobber(HexCoord(0, 0))

        assertTrue(victims.isEmpty(), "No victims when tile has no buildings")
    }

    @Test
    fun `robber location updates correctly`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)

        assertEquals(HexCoord(0, 0), board.robberLocation, "Initial robber location")

        board.moveRobber(HexCoord(1, 0))

        assertEquals(HexCoord(1, 0), board.robberLocation, "Robber should move to new location")
    }

    @Test
    fun `can move robber to same location - returns affected players`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "sheep", 10)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        board.moveRobber(HexCoord(0, 0))
        val victims = board.moveRobber(HexCoord(0, 0))

        assertTrue(victims.contains(TestHelpers.PLAYER_1))
    }

    @Test
    fun `robber blocks production for all players on tile`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wheat", 11)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        board.moveRobber(HexCoord(0, 0))

        val production = board.getProductionForRoll(11)

        assertEquals(0, production[TestHelpers.PLAYER_1]?.get("wheat") ?: 0)
        assertEquals(0, production[TestHelpers.PLAYER_2]?.get("wheat") ?: 0)
    }

    @Test
    fun `moving robber away restores production`() {
        val board = TestHelpers.createEmptyBoard()
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)

        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Block wood
        board.moveRobber(HexCoord(0, 0))
        var production = board.getProductionForRoll(6)
        assertEquals(0, production[TestHelpers.PLAYER_1]?.get("wood") ?: 0)

        // Move robber away
        board.moveRobber(HexCoord(1, 0))
        production = board.getProductionForRoll(6)
        assertEquals(1, production[TestHelpers.PLAYER_1]?.get("wood"))
    }
}

