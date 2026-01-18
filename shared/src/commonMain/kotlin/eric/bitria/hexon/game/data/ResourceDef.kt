package eric.bitria.hexon.game.data

import kotlinx.serialization.Serializable

@Serializable
data class ResourceDef(
    val id: ResourceId,       // "wood"
    val name: String,         // "Lumber"
    val color: String         // "#2D5A27"
)