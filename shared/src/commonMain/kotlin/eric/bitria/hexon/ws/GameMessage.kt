package eric.bitria.hexon.ws

import kotlinx.serialization.Serializable

@Serializable
sealed class GameMessage {
    abstract val senderId: String?
}