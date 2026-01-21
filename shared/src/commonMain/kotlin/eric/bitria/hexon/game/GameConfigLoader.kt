package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.def.ResourceDef
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.config.BoardConfig
import eric.bitria.hexon.game.data.config.FixedTile
import eric.bitria.hexon.game.data.config.GameConfig
import kotlin.math.max
import kotlin.math.min

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
        // 1. Generate Standard Coordinates (Radius 2)
        val coords = generateHexGrid(2)

        // 2. Resources
        val resources = mutableListOf<ResourceId?>().apply {
            repeat(4) { add("wood") }
            repeat(4) { add("sheep") }
            repeat(4) { add("wheat") }
            repeat(3) { add("brick") }
            repeat(3) { add("ore") }
        }

        // 3. Tokens
        val numbers = listOf(5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8, 10, 9, 4, 5, 6, 3, 11)

        // 4. Ports
        val ports = mutableListOf<PortDef>().apply {
            add(PortDef(HexCoord(0, 2),  HexCoord(-1, 2), HexCoord(0, 1),  null, 3))
            add(PortDef(HexCoord(2, -1), HexCoord(2, -2), HexCoord(1, -1), null, 3))
            add(PortDef(HexCoord(-1, -1),HexCoord(-2, 0), HexCoord(-1, 0), null, 3))
            add(PortDef(HexCoord(0, -2), HexCoord(1, -2), HexCoord(0, -1), null, 3))
            add(PortDef(HexCoord(1, 1),  HexCoord(1, 2),  HexCoord(2, 1),  "sheep", 2))
            add(PortDef(HexCoord(-2, 2), HexCoord(-2, 1), HexCoord(-1, 1), "brick", 2))
            add(PortDef(HexCoord(2, 0),  HexCoord(3, -1), HexCoord(2, -1), "ore", 2))
            add(PortDef(HexCoord(-1, -2),HexCoord(-2, -1),HexCoord(-1, -1),"wood", 2))
            add(PortDef(HexCoord(-2, 0), HexCoord(-3, 1), HexCoord(-2, 1), "wheat", 2))
        }

        return BoardConfig(
            coords = coords, // List passed here
            resources = resources,
            numbers = numbers,
            fixedTiles = mapOf(
                HexCoord(0, 0) to FixedTile(resource = null, number = null)
            ),
            ports = ports
        )
    }

    // --- Helper to generate grid ---
    private fun generateHexGrid(radius: Int): List<HexCoord> {
        val coords = mutableListOf<HexCoord>()
        for (q in -radius..radius) {
            val r1 = max(-radius, -q - radius)
            val r2 = min(radius, -q + radius)
            for (r in r1..r2) {
                coords.add(HexCoord(q, r))
            }
        }
        return coords
    }

    // Data Definitions

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