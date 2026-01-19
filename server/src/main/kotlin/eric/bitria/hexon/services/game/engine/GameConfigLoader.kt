package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.data.BuildingDef
import eric.bitria.hexon.game.data.GameConfig
import eric.bitria.hexon.game.data.PlacementType
import eric.bitria.hexon.game.data.ResourceDef

object GameConfigLoader {

    /**
     * Returns the standard 4-player Catan ruleset.
     */
    fun default(): GameConfig {
        return GameConfig(
            resources = defaultResources(),
            buildings = defaultBuildings()
        )
    }

    /**
     * Example: You could load this from a JSON file in resources
     */
    /*
    fun fromJson(jsonString: String): GameConfig {
        return Json.decodeFromString(jsonString)
    }
    */

    // ==========================================
    // DATA DEFINITIONS
    // ==========================================

    private fun defaultResources(): List<ResourceDef> {
        return listOf(
            ResourceDef(id = "wood",  name = "Lumber", color = "#2D5A27"),
            ResourceDef(id = "brick", name = "Brick",  color = "#B5391C"),
            ResourceDef(id = "sheep", name = "Wool",   color = "#8EC449"),
            ResourceDef(id = "wheat", name = "Grain",  color = "#F3C51F"),
            ResourceDef(id = "ore",   name = "Ore",    color = "#6C7A89")
        )
    }

    private fun defaultBuildings(): List<BuildingDef> {
        return listOf(
            // --- ROAD ---
            BuildingDef(
                id = "road",
                name = "Road",
                type = PlacementType.EDGE,
                cost = mapOf(
                    "wood" to 1,
                    "brick" to 1
                ),
                points = 0, // Points are calculated via "Longest Road", not inherent
                limitPerPlayer = 15
            ),

            // --- SETTLEMENT ---
            BuildingDef(
                id = "settlement",
                name = "Settlement",
                type = PlacementType.VERTEX,
                cost = mapOf(
                    "wood" to 1,
                    "brick" to 1,
                    "wheat" to 1,
                    "sheep" to 1
                ),
                points = 1,
                limitPerPlayer = 5
            ),

            // --- CITY ---
            BuildingDef(
                id = "city",
                name = "City",
                type = PlacementType.VERTEX,
                cost = mapOf(
                    "wheat" to 2,
                    "ore" to 3
                ),
                points = 2,
                limitPerPlayer = 4
            )
        )
    }
}