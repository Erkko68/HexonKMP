package eric.bitria.hexon.game.data.config

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.def.ResourceDef
import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val seed: String,

    // --- Definitions (Rules) ---
    val resourceDefs: List<ResourceDef>,
    val buildingDefs: List<BuildingDef>,

    // --- Board Layout (Geometry) ---
    val gridCoords: List<HexCoord>,
    val ports: List<PortDef>,

    // --- Deck / Pools (Randomization) ---
    // Renamed to avoid confusion with 'resourceDefs'
    val tileResourcePool: List<ResourceId?>,
    val tileNumberPool: List<Int>,

    // --- Overrides ---
    val fixedTiles: Map<HexCoord, FixedTile> = emptyMap()
)