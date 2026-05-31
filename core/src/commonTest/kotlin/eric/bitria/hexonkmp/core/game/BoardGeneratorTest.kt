package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.board.BoardGenerator
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BoardGeneratorTest {

    @Test
    fun classicBoardHas19TilesAndOneDesert() {
        val board = BoardGenerator.generate(ClassicCatan, seed = 1)
        assertEquals(19, board.tiles.size)
        assertEquals(1, board.tiles.count { it.terrain == Terrain.DESERT })
    }

    @Test
    fun everyNonDesertTileHasATokenAndDesertHasNone() {
        val board = BoardGenerator.generate(ClassicCatan, seed = 42)
        board.tiles.forEach { tile ->
            if (tile.terrain == Terrain.DESERT) assertEquals(null, tile.token)
            else assertNotNull(tile.token)
        }
    }

    @Test
    fun robberStartsOnTheDesert() {
        val board = BoardGenerator.generate(ClassicCatan, seed = 7)
        val desert = board.tiles.first { it.terrain == Terrain.DESERT }
        assertEquals(desert.hex, board.robber)
    }

    @Test
    fun generationIsDeterministicForSameSeed() {
        val a = BoardGenerator.generate(ClassicCatan, seed = 123)
        val b = BoardGenerator.generate(ClassicCatan, seed = 123)
        assertEquals(a, b)
    }

    @Test
    fun differentSeedsGenerallyProduceDifferentBoards() {
        val a = BoardGenerator.generate(ClassicCatan, seed = 1)
        val b = BoardGenerator.generate(ClassicCatan, seed = 2)
        assertTrue(a != b)
    }

    @Test
    fun classicBoardHas54VerticesAnd72Edges() {
        // A radius-2 hexagonal board has 54 corners and 72 edges.
        val board = BoardGenerator.generate(ClassicCatan, seed = 1)
        assertEquals(54, board.vertices().size)
        assertEquals(72, board.edges().size)
    }
}
