package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.config.FixedTile
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.def.ResourceDef
import kotlin.math.max
import kotlin.math.min

object GameConfigLoader {

    fun default(seed: String = "default"): GameConfig {
        return GameConfig(
            seed = seed,
            victoryPoints = 10,
            tradeRatio = 4,
            // Rules
            resourceDefs = defaultResourceDef(),
            buildingDefs = defaultBuildingDef(),
            // Geometry
            gridCoords = generateHexGrid(radius = 2),
            ports = defaultPorts(),
            // Pools
            tileResourcePool = defaultResourcePool(),
            tileNumberPool = listOf(5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8, 10, 9, 4, 5, 6, 3, 11),
            // Overrides
            fixedTiles = mapOf(
                HexCoord(0, 0) to FixedTile(resource = "desert", number = 0)
            )
        )
    }

    // HELPERS

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

    private fun defaultResourcePool(): List<ResourceId> {
        return mutableListOf<ResourceId>().apply {
            repeat(4) { add("wood") }
            repeat(4) { add("sheep") }
            repeat(4) { add("wheat") }
            repeat(3) { add("brick") }
            repeat(3) { add("ore") }
            add("desert")
        }
    }

    private fun defaultPorts(): List<PortDef> {
        return listOf(
            PortDef(HexCoord(0, 2),  HexCoord(-1, 2), HexCoord(0, 1),  null, 3),
            PortDef(HexCoord(2, -1), HexCoord(2, -2), HexCoord(1, -1), null, 3),
            PortDef(HexCoord(-1, -1),HexCoord(-2, 0), HexCoord(-1, 0), null, 3),
            PortDef(HexCoord(0, -2), HexCoord(1, -2), HexCoord(0, -1), null, 3),
            PortDef(HexCoord(1, 1),  HexCoord(1, 2),  HexCoord(2, 1),  "sheep", 2),
            PortDef(HexCoord(-2, 2), HexCoord(-2, 1), HexCoord(-1, 1), "brick", 2),
            PortDef(HexCoord(2, 0),  HexCoord(3, -1), HexCoord(2, -1), "ore", 2),
            PortDef(HexCoord(-1, -2),HexCoord(-2, -1),HexCoord(-1, -1),"wood", 2),
            PortDef(HexCoord(-2, 0), HexCoord(-3, 1), HexCoord(-2, 1), "wheat", 2)
        )
    }

    private fun defaultResourceDef(): List<ResourceDef> {
        return listOf(
            ResourceDef(id = "wood", name = "Lumber"),
            ResourceDef(id = "brick", name = "Brick"),
            ResourceDef(id = "sheep", name = "Wool"),
            ResourceDef(id = "wheat", name = "Grain"),
            ResourceDef(id = "ore", name = "Ore")
        )
    }

    private fun defaultBuildingDef(): List<BuildingDef> {
        return listOf(
            BuildingDef("road", "Road", PlacementType.EDGE, mapOf("wood" to 1, "brick" to 1), null, null, 0, 0, 15),
            BuildingDef("settlement", "Settlement", PlacementType.VERTEX, mapOf("wood" to 1, "brick" to 1, "wheat" to 1, "sheep" to 1), "city", null, 1, 1,10),
            BuildingDef("city", "City", PlacementType.VERTEX, mapOf("wheat" to 2, "ore" to 3), null, "settlement", 2, 2,10)
        )
    }
}