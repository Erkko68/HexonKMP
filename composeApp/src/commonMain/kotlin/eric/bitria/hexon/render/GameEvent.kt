package eric.bitria.hexon.render

import eric.bitria.hexon.game.data.HexCoord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface GameEvent {

    @Serializable
    @SerialName("Initialised")
    data object Initialised : GameEvent

    @Serializable
    data class PlacedBuilding(
        val buildingId: String,
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null
    ): GameEvent


}