package eric.bitria.hexon.render

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.def.PlacementType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface GameEvent {

    @Serializable
    data class PlacedBuilding(
        val buildingId: String,
        val type: PlacementType,
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null
    ): GameEvent
}