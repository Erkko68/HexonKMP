package eric.bitria.hexon.render

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface RenderEvent {

    @Serializable
    @SerialName("Initialised")
    data object Initialised : RenderEvent

}