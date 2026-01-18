package eric.bitria.hexon.game.data

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val resources: List<ResourceDef>,
    val buildings: List<BuildingDef>
)