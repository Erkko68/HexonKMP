package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord

/**
 * Shared test utilities for Board tests.
 */
object TestHelpers {

    /**
     * Creates a board initialized with the default Catan configuration.
     */
    fun createBoard(seed: String = "test_seed"): Board {
        val config = GameConfigLoader.default(seed)
        val board = Board()
        board.initialize(config)
        return board
    }

    /**
     * Creates a minimal empty board without any tiles.
     * Useful for testing specific tile configurations.
     */
    fun createEmptyBoard(): Board {
        val config = GameConfigLoader.default()
        val board = Board()
        // Only initialize definitions, not tiles
        config.resourceDefs.forEach { board.availableResources[it.id] = it }
        config.buildingDefs.forEach { board.availableBuildings[it.id] = it }
        return board
    }

    /**
     * Sets up a minimal map for basic placement tests.
     * Creates a small cluster of land tiles around the origin.
     */
    fun setupMinimalMap(board: Board) {
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)
        board.addTile(HexCoord(0, 1), "sheep", 4)
        board.addTile(HexCoord(1, -1), "wheat", 10)
        board.addTile(HexCoord(2, -1), "ore", 3)
        board.addTile(HexCoord(-1, 1), "wood", 5)
        board.addTile(HexCoord(-1, 0), "brick", 9)
    }

    /**
     * Sets up a larger map for comprehensive testing.
     */
    fun setupLargeMap(board: Board) {
        // Center ring
        board.addTile(HexCoord(0, 0), "wood", 6)

        // Inner ring (6 tiles)
        board.addTile(HexCoord(1, 0), "brick", 8)
        board.addTile(HexCoord(0, 1), "sheep", 4)
        board.addTile(HexCoord(-1, 1), "wheat", 10)
        board.addTile(HexCoord(-1, 0), "ore", 3)
        board.addTile(HexCoord(0, -1), "wood", 5)
        board.addTile(HexCoord(1, -1), "brick", 9)

        // Outer ring (12 tiles)
        board.addTile(HexCoord(2, -1), "sheep", 11)
        board.addTile(HexCoord(2, 0), "wheat", 4)
        board.addTile(HexCoord(1, 1), "ore", 6)
        board.addTile(HexCoord(0, 2), "wood", 8)
        board.addTile(HexCoord(-1, 2), "brick", 10)
        board.addTile(HexCoord(-2, 2), "sheep", 3)
        board.addTile(HexCoord(-2, 1), "wheat", 12)
        board.addTile(HexCoord(-2, 0), "ore", 2)
        board.addTile(HexCoord(-1, -1), "wood", 9)
        board.addTile(HexCoord(0, -2), "brick", 5)
        board.addTile(HexCoord(1, -2), "sheep", 11)
        board.addTile(HexCoord(2, -2), "wheat", 4)
    }

    // Building type IDs from the default config
    const val VILLAGE = "village"
    const val CITY = "city"
    const val ROAD = "road"

    // Player IDs for testing
    const val PLAYER_1 = "player1"
    const val PLAYER_2 = "player2"
    const val PLAYER_3 = "player3"
    const val PLAYER_4 = "player4"
}

