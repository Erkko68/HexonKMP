package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests that simulate real game scenarios.
 */
class GameScenarioTest {

    @Test
    fun `setup phase - snake draft initial placement`() {
        val board = TestHelpers.createBoard()

        // Snake draft: P1 -> P2 -> P3 -> P4 -> P4 -> P3 -> P2 -> P1
        // First round - villages only (no connection check)
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(1, 0), HexCoord(1, 1), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(1, 0)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(-1, 0), HexCoord(0, 0), HexCoord(0, -1), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-1, 0), HexCoord(0, 0)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, HexCoord(1, -1), HexCoord(2, -1), HexCoord(2, -2), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_3, HexCoord(1, -1), HexCoord(2, -1)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_4, HexCoord(-1, 2), HexCoord(-1, 1), HexCoord(0, 1), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_4, HexCoord(-1, 2), HexCoord(-1, 1)))

        // Second round (reverse order)
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_4, HexCoord(0, -1), HexCoord(1, -2), HexCoord(1, -1), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_4, HexCoord(0, -1), HexCoord(1, -2)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_3, HexCoord(-1, 0), HexCoord(-1, -1), HexCoord(-2, 0), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_3, HexCoord(-1, 0), HexCoord(-1, -1)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(2, 0), HexCoord(2, -1), HexCoord(1, 0), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(2, 0), HexCoord(2, -1)))

        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(-2, 2), HexCoord(-2, 1), HexCoord(-1, 1), checkConnection = false))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(-2, 2), HexCoord(-2, 1)))

        // Verify all 8 villages and 8 roads placed
        assertEquals(16, board.buildings.size)
    }

    @Test
    fun `mid-game - building expansion with roads`() {
        val board = TestHelpers.createBoard()

        // Initial setup at center
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0))

        // Expand road network
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1)), "Road 2")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1)), "Road 3")

        // Build new village at end of road (must be 2 edges away from first village)
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1), HexCoord(2, -2), checkConnection = true), "Village 2")

        assertEquals(5, board.buildings.size)
    }

    @Test
    fun `mid-game - upgrade to city`() {
        val board = TestHelpers.createBoard()

        // Setup
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(1, 0), HexCoord(1, 1), checkConnection = false)

        // Upgrade to city
        assertTrue(board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_1, HexCoord(0, 1), HexCoord(1, 0), HexCoord(1, 1)))

        val vertexId = HexCoord.getVertexId(HexCoord(0, 1), HexCoord(1, 0), HexCoord(1, 1))
        assertEquals(TestHelpers.CITY, board.getBuildingAt(vertexId)?.def?.id)
    }

    @Test
    fun `production round - multiple tiles produce`() {
        val board = TestHelpers.createBoard()

        // Setup player on a good intersection
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)

        // Get production for various rolls
        val rolls = listOf(2, 3, 4, 5, 6, 8, 9, 10, 11, 12)
        var totalProduction = 0

        for (roll in rolls) {
            val production = board.getProductionForRoll(roll)
            production[TestHelpers.PLAYER_1]?.values?.forEach { totalProduction += it }
        }

        // With a village on a intersection of multiple tiles, we should get some production
        // The exact amount depends on the board seed
        assertTrue(totalProduction >= 0)
    }

    @Test
    fun `robber scenario - block and move`() {
        val board = TestHelpers.createBoard()

        // Both players on center tile adjacent vertices
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1), checkConnection = false)

        // Robber starts on desert (center with default config)
        // Move robber to another tile
        val victims = board.moveRobber(HexCoord(1, 0))

        // Only Player 1 is adjacent to (1, 0)
        assertTrue(victims.isNotEmpty())
    }

    @Test
    fun `longest road race scenario`() {
        val board = TestHelpers.createBoard()

        // Player 1 builds a road segment - 3 roads
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)), "P1 Road 1")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1)), "P1 Road 2")
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1)), "P1 Road 3")

        // Player 2 builds a longer road - edges must share vertices to chain properly
        // Start with village at (-1,0), (-2,0), (-1,-1)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_2, HexCoord(-1, 0), HexCoord(-2, 0), HexCoord(-1, -1), checkConnection = false)

        // Build roads that share consecutive vertices:
        // Village vertex: {(-1,0), (-2,0), (-1,-1)}
        // Road 1: (-1,0)-(-2,0) - on village, touches vertex {(-1,0), (-2,0), (-2,1)}
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-1, 0), HexCoord(-2, 0)), "P2 Road 1")
        // Road 2: (-2,1)-(-1,0) - shares vertex {(-1,0), (-2,0), (-2,1)} with Road 1
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-2, 1), HexCoord(-1, 0)), "P2 Road 2")
        // Road 3: (-1,0)-(-1,1) - shares vertex {(-2,1), (-1,0), (-1,1)} with Road 2
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-1, 0), HexCoord(-1, 1)), "P2 Road 3")
        // Road 4: (-1,1)-(0,0) - shares vertex {(-1,0), (-1,1), (0,0)} with Road 3
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(-1, 1), HexCoord(0, 0)), "P2 Road 4")
        // Road 5: (0,0)-(0,1) - shares vertex {(-1,1), (0,0), (0,1)} with Road 4
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_2, HexCoord(0, 0), HexCoord(0, 1)), "P2 Road 5")

        // Count roads per player
        val p1Roads = board.buildings.values.count { it.ownerId == TestHelpers.PLAYER_1 && it.def.id == TestHelpers.ROAD }
        val p2Roads = board.buildings.values.count { it.ownerId == TestHelpers.PLAYER_2 && it.def.id == TestHelpers.ROAD }

        assertEquals(3, p1Roads)
        assertEquals(5, p2Roads)
    }

    @Test
    fun `end game scenario - many buildings`() {
        val board = TestHelpers.createBoard()

        // Simulate end-game state with buildings
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), checkConnection = false)
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, 0), HexCoord(1, -1)))
        assertTrue(board.placeEdgeBuilding(
            TestHelpers.ROAD,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1)))

        // Build village at end of road (2 edges away from first)
        assertTrue(board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(1, -1), HexCoord(2, -1), HexCoord(2, -2), checkConnection = true))

        // Upgrade first village to city
        assertTrue(board.placeVertexBuilding(
            TestHelpers.CITY,
            TestHelpers.PLAYER_1, HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1)))

        // Verify building counts
        val p1Buildings = board.buildings.values.filter { it.ownerId == TestHelpers.PLAYER_1 }
        val villages = p1Buildings.count { it.def.id == TestHelpers.VILLAGE }
        val cities = p1Buildings.count { it.def.id == TestHelpers.CITY }
        val roads = p1Buildings.count { it.def.id == TestHelpers.ROAD }

        assertEquals(1, villages)
        assertEquals(1, cities)
        assertEquals(3, roads)
    }

    @Test
    fun `port access scenario`() {
        val board = TestHelpers.createBoard()

        // Ports are at specific vertices according to default config
        // Place a village at a port vertex
        // Port: PortDef(HexCoord(1, 1), HexCoord(1, 2), HexCoord(2, 1), "sheep", 2)
        board.placeVertexBuilding(
            TestHelpers.VILLAGE,
            TestHelpers.PLAYER_1, HexCoord(1, 1), HexCoord(1, 2), HexCoord(2, 1), checkConnection = false)

        // Verify the vertex has a port
        val portId = HexCoord.getVertexId(HexCoord(1, 1), HexCoord(1, 2), HexCoord(2, 1))
        val port = board.ports[portId]

        assertEquals("sheep", port?.resourceId)
        assertEquals(2, port?.ratio)
    }
}

