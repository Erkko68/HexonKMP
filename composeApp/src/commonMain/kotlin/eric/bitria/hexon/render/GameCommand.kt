package eric.bitria.hexon.render

import kotlinx.serialization.Serializable

sealed interface GameCommand {

    @Serializable
    data class UpdateSpeed(val speed: Float) : GameCommand

    @Serializable
    data class MoveCamera(val x: Float, val y: Float) : GameCommand
}