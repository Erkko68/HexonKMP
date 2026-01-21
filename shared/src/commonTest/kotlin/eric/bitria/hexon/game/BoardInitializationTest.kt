package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BoardInitializationTest {

    // Helper updated for flattened GameConfig
    private fun createInitializedBoard(seed: String = "default_test_seed"): Board {
        val config = GameConfigLoader.default(seed)
        // Pass definitions from the flat config
        val board = Board(config.resourceDefs, config.buildingDefs)
        // Pass the full config to initialize (which contains the seed)
        board.initialize(config)
        return board
    }

    @Test
    fun `Test Deterministic Generation (Seeding)`() {
        val seed = "MATCH_ID_12345"

        // Initialize two separate boards with the SAME seed
        val board1 = createInitializedBoard(seed)
        val board2 = createInitializedBoard(seed)

        // 1. Verify Tiles match exactly
        assertEquals(board1.tiles.size, board2.tiles.size)

        // We compare the internal maps. Since HexTile is a data class,
        // this checks that every coordinate has the exact same resource and number.
        assertEquals(board1.tiles, board2.tiles, "Boards with the same seed must be identical")

        // 2. Verify Robber location matches
        assertEquals(board1.robberLocation, board2.robberLocation)
    }

    @Test
    fun `Test Randomness (Different Seeds)`() {
        // This test ensures we aren't just generating the same static board every time
        val board1 = createInitializedBoard("SEED_A")
        val board2 = createInitializedBoard("SEED_B")

        // It is statistically nearly impossible for two shuffled Catan boards to match
        assertNotEquals(board1.tiles, board2.tiles, "Different seeds should produce different board layouts")
    }

    @Test
    fun `Test Initial Tile Count (Radius 2)`() {
        val board = createInitializedBoard()
        assertEquals(19, board.tiles.size, "Board must generate exactly 19 hex tiles")
    }

    @Test
    fun `Test Initial Port Count`() {
        val board = createInitializedBoard()
        assertEquals(9, board.ports.size, "Board must generate exactly 9 ports")
    }

    @Test
    fun `Test Desert Generation`() {
        val board = createInitializedBoard()

        // Find tiles with null resource (Desert)
        val desertTiles = board.tiles.values.filter { it.resourceId == null }

        assertEquals(1, desertTiles.size, "There must be exactly one Desert tile")
        assertEquals(0, desertTiles.first().numberToken, "Desert must have number token 0")
    }

    @Test
    fun `Test Robber Starts on Desert`() {
        val board = createInitializedBoard()

        val desertTile = board.tiles.values.first { it.resourceId == null }

        assertEquals(desertTile.coordinate, board.robberLocation, "Robber should be initialized at the Desert coordinate")
    }

    @Test
    fun `Test Fixed Tile at Center`() {
        val board = createInitializedBoard()

        val centerId = HexCoord.getHexId(HexCoord(0, 0))
        val centerTile = board.tiles[centerId]

        assertNotNull(centerTile, "Center tile (0,0) must exist")
        assertNull(centerTile.resourceId, "Center tile must be the Desert (null resource)")
    }

    @Test
    fun `Test Resource Distribution Counts`() {
        val board = createInitializedBoard()

        val counts = board.tiles.values
            .mapNotNull { it.resourceId }
            .groupingBy { it }
            .eachCount()

        assertEquals(4, counts["wood"], "Should have 4 Wood")
        assertEquals(4, counts["sheep"], "Should have 4 Sheep")
        assertEquals(4, counts["wheat"], "Should have 4 Wheat")
        assertEquals(3, counts["brick"], "Should have 3 Brick")
        assertEquals(3, counts["ore"],   "Should have 3 Ore")
    }

    @Test
    fun `Test Token Number Distribution`() {
        val board = createInitializedBoard()

        val actualNumbers = board.tiles.values
            .map { it.numberToken }
            .filter { it != 0 }
            .sorted()

        val expectedNumbers = listOf(
            2, 3, 3, 4, 4, 5, 5, 6, 6,
            8, 8, 9, 9, 10, 10, 11, 11, 12
        )

        assertEquals(expectedNumbers, actualNumbers, "Token numbers must match the standard probability distribution")
    }
}