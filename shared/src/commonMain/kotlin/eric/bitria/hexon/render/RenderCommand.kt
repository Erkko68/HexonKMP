package eric.bitria.hexon.render

import kotlinx.serialization.Serializable

sealed interface RenderCommand {
    @Serializable
    data object ClearScreen : RenderCommand

}