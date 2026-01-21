package eric.bitria.hexon.game.data.def

import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
data class ResourceDef(
    val id: ResourceId,       // "wood"
    val name: String         // "Lumber"
)