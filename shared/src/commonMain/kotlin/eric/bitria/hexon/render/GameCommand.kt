package eric.bitria.hexon.render

import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.PortDef
import kotlinx.serialization.Serializable

sealed interface GameCommand {

    // Gameplay

    @Serializable
    data class PlaceBuilding(
        val player: PlayerId, val buildingId: BuildingId,
        val color: String,
        val placementType: PlacementType,
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null
    ) : GameCommand

    @Serializable
    data class DiceRolled(
        val values: Pair<Int,Int>,
        val sum: Int
    ) : GameCommand

    @Serializable
    data class RobberUpdated(
        val location: HexCoord
    ) : GameCommand

    // Board Initialization

    @Serializable
    data class SetHex(
        val coord: HexCoord,
        val resource: ResourceId,
        val number: Int
    ) : GameCommand

    @Serializable
    data class SetPort(
        val port: PortDef
    ) : GameCommand

    // Display Building Positions

    @Serializable
    data class ShowVertexBuildingPositions(
        val buildingId: BuildingId,
        val availablePositions: List<Triple<HexCoord, HexCoord, HexCoord>>
    ) : GameCommand

    @Serializable
    data class ShowEdgeBuildingPositions(
        val buildingId: BuildingId,
        val availablePositions: List<Pair<HexCoord, HexCoord>>
    ) : GameCommand

}