package eric.bitria.hexon.game.data

import kotlinx.serialization.Serializable

@Serializable
data class BuildingDef(
    val id: BuildingId,       // "settlement"
    val name: String,         // "Settlement"
    val type: PlacementType,  // VERTEX or EDGE
    val cost: Map<ResourceId, Int>, // {"wood": 1, "brick": 1, ...}
    val points: Int,          // 1
    val limitPerPlayer: Int   // 5
)

@Serializable
enum class PlacementType {
    EDGE,   // Roads, Ships (2 Coords)
    VERTEX  // Settlements, Cities (3 Coords)
}