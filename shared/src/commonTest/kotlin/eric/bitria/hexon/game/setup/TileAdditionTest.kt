package eric.bitria.hexon.game.setup

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for adding tiles to the board.
 */
class TileAdditionTest {

    @Test
    fun `addTile creates tile at correct coordinate`() {
        val board = TestHelpers.createEmptyBoard()
        val coord = HexCoord(0, 0)

        board.addTile(coord, "wood", 6)

        val tileId = HexCoord.getHexId(coord)
        assertNotNull(board.tiles[tileId])
        assertEquals(coord, board.tiles[tileId]?.coordinate)
    }

    @Test
    fun `addTile sets correct resource type`() {
        val board = TestHelpers.createEmptyBoard()
        val coord = HexCoord(0, 0)

        board.addTile(coord, "brick", 8)

        val tileId = HexCoord.getHexId(coord)
        assertEquals("brick", board.tiles[tileId]?.resourceId)
    }

    @Test
    fun `addTile sets correct number token`() {
        val board = TestHelpers.createEmptyBoard()
        val coord = HexCoord(0, 0)

        board.addTile(coord, "sheep", 10)

        val tileId = HexCoord.getHexId(coord)
        assertEquals(10, board.tiles[tileId]?.numberToken)
    }

    @Test
    fun `adding desert tile moves robber`() {
        val board = TestHelpers.createEmptyBoard()
        val desertCoord = HexCoord(2, -1)
        val initialRobber = board.robberLocation

        board.addTile(desertCoord, "desert", 0)

        assertEquals(desertCoord, board.robberLocation, "Robber should move to desert")
    }

    @Test
    fun `adding non-desert tile does not move robber`() {
        val board = TestHelpers.createEmptyBoard()
        val initialRobber = board.robberLocation

        board.addTile(HexCoord(1, 1), "wood", 6)

        assertEquals(initialRobber, board.robberLocation, "Robber should not move for non-desert tiles")
    }

    @Test
    fun `adding multiple desert tiles moves robber to last one`() {
        val board = TestHelpers.createEmptyBoard()

        board.addTile(HexCoord(0, 0), "desert", 0)
        board.addTile(HexCoord(1, 0), "desert", 0)

        assertEquals(HexCoord(1, 0), board.robberLocation)
    }

    @Test
    fun `can add all standard resource types`() {
        val board = TestHelpers.createEmptyBoard()
        val resources = listOf("wood", "brick", "sheep", "wheat", "ore", "desert")

        resources.forEachIndexed { index, resource ->
            board.addTile(HexCoord(index, 0), resource, index + 2)
        }

        assertEquals(6, board.tiles.size)
    }

    @Test
    fun `tiles can have same number token`() {
        val board = TestHelpers.createEmptyBoard()

        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 6)

        assertEquals(2, board.tiles.size)
        assertTrue(board.tiles.values.all { it.numberToken == 6 })
    }

    @Test
    fun `tiles can be added at negative coordinates`() {
        val board = TestHelpers.createEmptyBoard()

        board.addTile(HexCoord(-3, -2), "wood", 5)
        board.addTile(HexCoord(-1, 2), "ore", 9)

        assertEquals(2, board.tiles.size)
        assertNotNull(board.tiles[HexCoord.getHexId(HexCoord(-3, -2))])
        assertNotNull(board.tiles[HexCoord.getHexId(HexCoord(-1, 2))])
    }

    @Test
    fun `overwriting tile at same coordinate replaces it`() {
        val board = TestHelpers.createEmptyBoard()
        val coord = HexCoord(0, 0)

        board.addTile(coord, "wood", 6)
        board.addTile(coord, "brick", 8)

        assertEquals(1, board.tiles.size)
        assertEquals("brick", board.tiles[HexCoord.getHexId(coord)]?.resourceId)
        assertEquals(8, board.tiles[HexCoord.getHexId(coord)]?.numberToken)
    }
}

