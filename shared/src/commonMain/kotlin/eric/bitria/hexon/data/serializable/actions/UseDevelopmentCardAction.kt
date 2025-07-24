package eric.bitria.hexon.data.serializable.actions

import eric.bitria.hexon.data.HexPosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("USE_DEV_CARD")
data class UseDevelopmentCardAction(
    override val type: String = "USE_DEV_CARD",
    val playerId: String,
    val card: DevelopmentCardType,
    val targetPlayerId: String? = null,
    val position: HexPosition
) : HexonAction()

@Serializable
enum class DevelopmentCardType {
    KNIGHT,
    MONOPOLY,
    ROAD_BUILDING,
    YEAR_OF_PLENTY,
    VICTORY_POINT
}

