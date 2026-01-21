package eric.bitria.hexon.game.data.config

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
data class BoardConfig(
    val coords: List<HexCoord>,        // Explicit list of valid tile coordinates
    val resources: List<ResourceId?>, // List of resource IDs (null for Desert)
    val numbers: List<Int>,         // List of number tokens
    val fixedTiles: Map<HexCoord, FixedTile> = emptyMap(), // Optional overrides
    val ports: List<PortDef> = emptyList() // NEW: Ports definition
)
