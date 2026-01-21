package eric.bitria.hexon.game.data.config

import eric.bitria.hexon.game.FixedTile
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
data class BoardConfig(
    val mapRadius: Int,             // Radius of the hex grid (2 = 19 tiles)
    val resources: List<ResourceId?>, // List of resource IDs (null for Desert)
    val numbers: List<Int>,         // List of number tokens
    val fixedTiles: Map<HexCoord, FixedTile> = emptyMap() // Optional overrides
)
