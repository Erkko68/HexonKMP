package eric.bitria.hexon.game.ports

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for port placement and configuration.
 */
class PortTest {

    @Test
    fun `addPort creates port at correct vertex`() {
        val board = TestHelpers.createEmptyBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.addPort(h1, h2, h3, "wood", 2)

        val portId = HexCoord.getVertexId(h1, h2, h3)
        assertNotNull(board.ports[portId])
    }

    @Test
    fun `port has correct resource type`() {
        val board = TestHelpers.createEmptyBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.addPort(h1, h2, h3, "brick", 2)

        val portId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals("brick", board.ports[portId]?.resourceId)
    }

    @Test
    fun `port has correct ratio`() {
        val board = TestHelpers.createEmptyBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.addPort(h1, h2, h3, "ore", 2)

        val portId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals(2, board.ports[portId]?.ratio)
    }

    @Test
    fun `generic port has null resource`() {
        val board = TestHelpers.createEmptyBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.addPort(h1, h2, h3, null, 3)

        val portId = HexCoord.getVertexId(h1, h2, h3)
        assertNull(board.ports[portId]?.resourceId)
        assertEquals(3, board.ports[portId]?.ratio)
    }

    @Test
    fun `default board has 9 ports`() {
        val board = TestHelpers.createBoard()
        assertEquals(9, board.ports.size, "Default board should have 9 ports")
    }

    @Test
    fun `default board has 4 generic ports`() {
        val board = TestHelpers.createBoard()
        val genericPorts = board.ports.values.filter { it.resourceId == null }
        assertEquals(4, genericPorts.size, "Should have 4 generic 3:1 ports")
    }

    @Test
    fun `default board has 5 resource-specific ports`() {
        val board = TestHelpers.createBoard()
        val specificPorts = board.ports.values.filter { it.resourceId != null }
        assertEquals(5, specificPorts.size, "Should have 5 resource-specific 2:1 ports")
    }

    @Test
    fun `generic ports have 3 to 1 ratio`() {
        val board = TestHelpers.createBoard()
        val genericPorts = board.ports.values.filter { it.resourceId == null }
        assertTrue(genericPorts.all { it.ratio == 3 }, "All generic ports should be 3:1")
    }

    @Test
    fun `resource-specific ports have 2 to 1 ratio`() {
        val board = TestHelpers.createBoard()
        val specificPorts = board.ports.values.filter { it.resourceId != null }
        assertTrue(specificPorts.all { it.ratio == 2 }, "All resource ports should be 2:1")
    }

    @Test
    fun `each resource has one dedicated port`() {
        val board = TestHelpers.createBoard()
        val resourcePorts = board.ports.values
            .mapNotNull { it.resourceId }
            .groupingBy { it }
            .eachCount()

        assertEquals(1, resourcePorts["wood"], "Should have 1 wood port")
        assertEquals(1, resourcePorts["brick"], "Should have 1 brick port")
        assertEquals(1, resourcePorts["sheep"], "Should have 1 sheep port")
        assertEquals(1, resourcePorts["wheat"], "Should have 1 wheat port")
        assertEquals(1, resourcePorts["ore"], "Should have 1 ore port")
    }

    @Test
    fun `port id is order-independent`() {
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        val id1 = HexCoord.getVertexId(h1, h2, h3)
        val id2 = HexCoord.getVertexId(h3, h1, h2)
        val id3 = HexCoord.getVertexId(h2, h3, h1)

        assertEquals(id1, id2)
        assertEquals(id2, id3)
    }

    @Test
    fun `can add multiple ports`() {
        val board = TestHelpers.createEmptyBoard()

        board.addPort(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1), "wood", 2)
        board.addPort(HexCoord(2, 0), HexCoord(2, 1), HexCoord(1, 1), null, 3)
        board.addPort(HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(0, 0), "ore", 2)

        assertEquals(3, board.ports.size)
    }

    @Test
    fun `overwriting port at same vertex replaces it`() {
        val board = TestHelpers.createEmptyBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.addPort(h1, h2, h3, "wood", 2)
        board.addPort(h1, h2, h3, "brick", 3) // Overwrite

        assertEquals(1, board.ports.size)
        val portId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals("brick", board.ports[portId]?.resourceId)
        assertEquals(3, board.ports[portId]?.ratio)
    }
}

