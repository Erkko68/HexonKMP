package eric.bitria.hexon.render

import kotlinx.serialization.Serializable

sealed interface GameEvent {
    @Serializable
    data object Initialised : GameEvent

    @Serializable
    data class ObjectClicked(val id: String, val type: String) : GameEvent

    @Serializable
    data object AnimationFinished : GameEvent
}