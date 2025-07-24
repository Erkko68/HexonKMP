package eric.bitria.hexon.data.serializable.actions

import eric.bitria.hexon.data.HexPosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PLACE_ROBBER")
data class PlaceRobberAction(
    override val type: String = "PLACE_ROBBER",
    val playerId: String,
    val position: HexPosition
) : HexonAction()
