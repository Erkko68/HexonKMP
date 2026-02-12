package eric.bitria.hexon.game.setup

import eric.bitria.hexon.game.TestHelpers
import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Tests for board initialization and seeding.
 */
class BoardInitializationTest {

    @Test
    fun `initialize creates correct number of tiles for radius 2`() {
        val board = TestHelpers.createBoard()
        assertEquals(19, board.tiles.size, "Board must generate exactly 19 hex tiles for radius 2")
    }

    @Test
    fun `initialize creates correct number of ports`() {
        val board = TestHelpers.createBoard()
        assertEquals(9, board.ports.size, "Board must generate exactly 9 ports")
    }

    @Test
    fun `same seed produces identical boards`() {
        val seed = "DETERMINISTIC_SEED_12345"
        val board1 = TestHelpers.createBoard(seed)
        val board2 = TestHelpers.createBoard(seed)

        assertEquals(board1.tiles.size, board2.tiles.size)
        assertEquals(board1.tiles, board2.tiles, "Boards with the same seed must be identical")
        assertEquals(board1.robberLocation, board2.robberLocation)
    }

    @Test
    fun `different seeds produce different boards`() {
        val board1 = TestHelpers.createBoard("SEED_A")
        val board2 = TestHelpers.createBoard("SEED_B")

        assertNotEquals(board1.tiles, board2.tiles, "Different seeds should produce different board layouts")
    }

    @Test
    fun `desert tile has no resource`() {
        val board = TestHelpers.createBoard()
        val desertTiles = board.tiles.values.filter { it.resourceId == "desert" }

        assertEquals(1, desertTiles.size, "There must be exactly one Desert tile")
        assertEquals(0, desertTiles.first().numberToken, "Desert must have number token 0")
    }

    @Test
    fun `robber starts on desert`() {
        val board = TestHelpers.createBoard()
        val desertTile = board.tiles.values.first { it.resourceId == "desert" }

        assertEquals(desertTile.coordinate, board.robberLocation, "Robber should start on desert")
    }

    @Test
    fun `center tile is desert with fixed config`() {
        val board = TestHelpers.createBoard()
        val centerId = HexCoord.getHexId(HexCoord(0, 0))
        val centerTile = board.tiles[centerId]

        assertNotNull(centerTile, "Center tile (0,0) must exist")
        assertEquals("desert", centerTile.resourceId, "Center tile must be the Desert")
    }

    @Test
    fun `resource distribution matches Catan standard`() {
        val board = TestHelpers.createBoard()

        val counts = board.tiles.values
            .filter { it.resourceId != "desert" }
            .groupingBy { it.resourceId }
            .eachCount()

        assertEquals(4, counts["wood"], "Should have 4 Wood")
        assertEquals(4, counts["sheep"], "Should have 4 Sheep")
        assertEquals(4, counts["wheat"], "Should have 4 Wheat")
        assertEquals(3, counts["brick"], "Should have 3 Brick")
        assertEquals(3, counts["ore"], "Should have 3 Ore")
    }

    @Test
    fun `number token distribution matches Catan standard`() {
        val board = TestHelpers.createBoard()

        val actualNumbers = board.tiles.values
            .map { it.numberToken }
            .filter { it != 0 }
            .sorted()

        val expectedNumbers = listOf(
            2, 3, 3, 4, 4, 5, 5, 6, 6,
            8, 8, 9, 9, 10, 10, 11, 11, 12
        )

        assertEquals(expectedNumbers, actualNumbers, "Token numbers must match standard probability distribution")
    }

    @Test
    fun `available buildings are registered after initialization`() {
        val board = TestHelpers.createBoard()

        assertNotNull(board.availableBuildings["village"], "Village building should be registered")
        assertNotNull(board.availableBuildings["city"], "City building should be registered")
        assertNotNull(board.availableBuildings["road"], "Road building should be registered")
    }

    @Test
    fun `available resources are registered after initialization`() {
        val board = TestHelpers.createBoard()

        assertNotNull(board.availableResources["wood"], "Wood resource should be registered")
        assertNotNull(board.availableResources["brick"], "Brick resource should be registered")
        assertNotNull(board.availableResources["sheep"], "Sheep resource should be registered")
        assertNotNull(board.availableResources["wheat"], "Wheat resource should be registered")
        assertNotNull(board.availableResources["ore"], "Ore resource should be registered")
    }
}

