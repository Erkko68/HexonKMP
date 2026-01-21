package eric.bitria.hexon.game.data.config

import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
data class FixedTile(
    val resource: ResourceId?, // Force a specific resource here
    val number: Int? = null    // Optional: Force a specific number here
)