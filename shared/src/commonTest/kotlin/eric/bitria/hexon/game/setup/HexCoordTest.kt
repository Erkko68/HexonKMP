package eric.bitria.hexon.game.setup

import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for HexCoord functionality and geometric calculations.
 */
class HexCoordTest {

    @Test
    fun `hex id is consistent`() {
        val coord = HexCoord(3, -2)
        val id1 = HexCoord.getHexId(coord)
        val id2 = HexCoord.getHexId(coord)
        assertEquals(id1, id2)
    }

    @Test
    fun `different hexes have different ids`() {
        val id1 = HexCoord.getHexId(HexCoord(0, 0))
        val id2 = HexCoord.getHexId(HexCoord(0, 1))
        val id3 = HexCoord.getHexId(HexCoord(1, 0))
        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertNotEquals(id2, id3)
    }

    @Test
    fun `edge id is order-independent`() {
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        assertEquals(HexCoord.getEdgeId(h1, h2), HexCoord.getEdgeId(h2, h1))
    }

    @Test
    fun `different edges have different ids`() {
        val id1 = HexCoord.getEdgeId(HexCoord(0, 0), HexCoord(1, 0))
        val id2 = HexCoord.getEdgeId(HexCoord(0, 0), HexCoord(0, 1))
        val id3 = HexCoord.getEdgeId(HexCoord(1, 0), HexCoord(0, 1))
        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertNotEquals(id2, id3)
    }

    @Test
    fun `vertex id is order-independent for all permutations`() {
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        val id = HexCoord.getVertexId(h1, h2, h3)
        assertEquals(id, HexCoord.getVertexId(h1, h3, h2))
        assertEquals(id, HexCoord.getVertexId(h2, h1, h3))
        assertEquals(id, HexCoord.getVertexId(h2, h3, h1))
        assertEquals(id, HexCoord.getVertexId(h3, h1, h2))
        assertEquals(id, HexCoord.getVertexId(h3, h2, h1))
    }

    @Test
    fun `different vertices have different ids`() {
        val id1 = HexCoord.getVertexId(HexCoord(0, 0), HexCoord(1, 0), HexCoord(0, 1))
        val id2 = HexCoord.getVertexId(HexCoord(0, 0), HexCoord(1, 0), HexCoord(1, -1))
        val id3 = HexCoord.getVertexId(HexCoord(0, 0), HexCoord(-1, 0), HexCoord(0, -1))
        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertNotEquals(id2, id3)
    }

    @Test
    fun `hexCoord compareTo orders by q first then r`() {
        val coords = listOf(
            HexCoord(1, 2),
            HexCoord(0, 0),
            HexCoord(0, 1),
            HexCoord(-1, 3),
            HexCoord(1, 0)
        ).sorted()

        assertEquals(HexCoord(-1, 3), coords[0])
        assertEquals(HexCoord(0, 0), coords[1])
        assertEquals(HexCoord(0, 1), coords[2])
        assertEquals(HexCoord(1, 0), coords[3])
        assertEquals(HexCoord(1, 2), coords[4])
    }

    @Test
    fun `hexCoord toString format is correct`() {
        assertEquals("0,0", HexCoord(0, 0).toString())
        assertEquals("1,-2", HexCoord(1, -2).toString())
        assertEquals("-3,4", HexCoord(-3, 4).toString())
    }

    @Test
    fun `hexCoord fromHexId parses correctly`() {
        assertEquals(HexCoord(0, 0), HexCoord.fromHexId("0,0"))
        assertEquals(HexCoord(1, -2), HexCoord.fromHexId("1,-2"))
        assertEquals(HexCoord(-3, 4), HexCoord.fromHexId("-3,4"))
    }

    @Test
    fun `hexCoord equality works correctly`() {
        assertEquals(HexCoord(1, 2), HexCoord(1, 2))
        assertNotEquals(HexCoord(1, 2), HexCoord(2, 1))
        assertNotEquals(HexCoord(1, 2), HexCoord(1, 3))
    }

    @Test
    fun `hexCoord hashCode is consistent with equals`() {
        val coord1 = HexCoord(1, 2)
        val coord2 = HexCoord(1, 2)
        assertEquals(coord1.hashCode(), coord2.hashCode())
    }

    @Test
    fun `negative coordinates work correctly`() {
        val h1 = HexCoord(-2, -3)
        val h2 = HexCoord(-1, -2)
        val h3 = HexCoord(-2, -2)

        val vertexId = HexCoord.getVertexId(h1, h2, h3)
        val edgeId = HexCoord.getEdgeId(h1, h2)

        // Just verify they don't crash and produce consistent IDs
        assertEquals(vertexId, HexCoord.getVertexId(h3, h1, h2))
        assertEquals(edgeId, HexCoord.getEdgeId(h2, h1))
    }

    @Test
    fun `large coordinates work correctly`() {
        val h1 = HexCoord(100, -50)
        val h2 = HexCoord(101, -50)
        val h3 = HexCoord(100, -49)

        val vertexId = HexCoord.getVertexId(h1, h2, h3)
        val edgeId = HexCoord.getEdgeId(h1, h2)

        assertEquals(vertexId, HexCoord.getVertexId(h2, h3, h1))
        assertEquals(edgeId, HexCoord.getEdgeId(h2, h1))
    }
}

