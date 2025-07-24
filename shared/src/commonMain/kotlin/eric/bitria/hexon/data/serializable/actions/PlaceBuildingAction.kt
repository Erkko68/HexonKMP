package eric.bitria.hexon.data.serializable.actions

import eric.bitria.hexon.data.HexPosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PLACE_BUILDING")
data class PlaceBuildingAction(
    override val type: String = "PLACE_BUILDING",
    val playerId: String,
    val buildingType: BuildingType,
    val position: HexPosition,
) : HexonAction()

@Serializable
enum class BuildingType {
    SETTLEMENT,
    CITY,
    ROAD
}