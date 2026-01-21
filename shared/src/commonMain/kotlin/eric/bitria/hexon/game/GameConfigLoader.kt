package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.BuildingDef
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlacementType
import eric.bitria.hexon.game.data.ResourceDef
import eric.bitria.hexon.game.data.config.BoardConfig
import eric.bitria.hexon.game.data.config.FixedTile
import eric.bitria.hexon.game.data.config.GameConfig

object GameConfigLoader {

    /**
     * Returns the standard 4-player Catan ruleset and board setup.
     */
    fun default(): GameConfig {
        return GameConfig(
            resources = defaultResources(),
            buildings = defaultBuildings(),
            board = defaultBoard()
        )
    }

    // ==========================================
    // BOARD DEFINITIONS
    // ==========================================

    private fun defaultBoard(): BoardConfig {
        // Standard Catan (19 Hexes)
        // 4 Wood, 4 Sheep, 4 Wheat, 3 Brick, 3 Ore, 1 Desert
        val resources = mutableListOf<String?>().apply {
            repeat(4) { add("wood") }
            repeat(4) { add("sheep") }
            repeat(4) { add("wheat") }
            repeat(3) { add("brick") }
            repeat(3) { add("ore") }
            add(null) // Desert
        }

        // Standard Tokens (18 Tokens, skipping 7/Desert)
        // 1x2, 2x3..11, 1x12
        val numbers = listOf(5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8, 10, 9, 4, 5, 6, 3, 11)

        return BoardConfig(
            mapRadius = 2, // Standard board has radius 2 (Center + Ring 1 + Ring 2)
            resources = resources,
            numbers = numbers,
            // Example: Fixing the Desert to the center (0,0)
            fixedTiles = mapOf(
                HexCoord(0, 0) to FixedTile(resource = null, number = null)
            )
        )
    }

    // ==========================================
    // GAME DATA DEFINITIONS
    // ==========================================

    private fun defaultResources(): List<ResourceDef> {
        return listOf(
            ResourceDef(id = "wood", name = "Lumber", color = "#2D5A27"),
            ResourceDef(id = "brick", name = "Brick", color = "#B5391C"),
            ResourceDef(id = "sheep", name = "Wool", color = "#8EC449"),
            ResourceDef(id = "wheat", name = "Grain", color = "#F3C51F"),
            ResourceDef(id = "ore", name = "Ore", color = "#6C7A89")
        )
    }

    private fun defaultBuildings(): List<BuildingDef> {
        return listOf(
            // --- ROAD ---
            BuildingDef(
                id = "road",
                name = "Road",
                type = PlacementType.EDGE,
                cost = mapOf("wood" to 1, "brick" to 1),
                points = 0,
                production = 0,
                limitPerPlayer = 15
            ),
            // --- SETTLEMENT ---
            BuildingDef(
                id = "settlement",
                name = "Settlement",
                type = PlacementType.VERTEX,
                cost = mapOf("wood" to 1, "brick" to 1, "wheat" to 1, "sheep" to 1),
                upgrade = "city",
                production = 1,
                points = 1,
                limitPerPlayer = 5
            ),
            // --- CITY ---
            BuildingDef(
                id = "city",
                name = "City",
                type = PlacementType.VERTEX,
                cost = mapOf("wheat" to 2, "ore" to 3),
                production = 2,
                points = 2,
                limitPerPlayer = 4
            )
        )
    }
}