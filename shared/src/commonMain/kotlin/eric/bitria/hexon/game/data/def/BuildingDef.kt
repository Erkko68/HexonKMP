package eric.bitria.hexon.game.data.def

import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
data class BuildingDef(
    val id: BuildingId,       // "settlement"
    val name: String,         // "Settlement"
    val type: PlacementType,  // VERTEX or EDGE
    val cost: Map<ResourceId, Int>, // {"wood": 1, "brick": 1, ...}
    val upgrade: BuildingId? = null, // Id of the upgrade of this building
    val downgrade: BuildingId? = null, // Id of the downgrade of this building
    val production: Int,
    val points: Int,          // 1
    val limitPerPlayer: Int   // 5
)

@Serializable
enum class PlacementType {
    EDGE,   // Roads, Ships (2 Coords)
    VERTEX  // Settlements, Cities (3 Coords)
}