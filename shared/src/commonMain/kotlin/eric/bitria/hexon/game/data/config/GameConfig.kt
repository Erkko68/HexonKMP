package eric.bitria.hexon.game.data.config

import eric.bitria.hexon.game.BoardConfig
import eric.bitria.hexon.game.data.BuildingDef
import eric.bitria.hexon.game.data.ResourceDef
import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val resources: List<ResourceDef>,
    val buildings: List<BuildingDef>,
    val board: BoardConfig
)