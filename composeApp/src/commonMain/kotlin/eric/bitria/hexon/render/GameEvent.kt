package eric.bitria.hexon.render

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface GameEvent {

    @Serializable
    @SerialName("Initialised")
    data object Initialised : GameEvent
}