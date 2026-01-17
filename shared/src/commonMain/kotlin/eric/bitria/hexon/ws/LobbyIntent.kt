package eric.bitria.hexon.ws

import kotlinx.serialization.Serializable
/**
 * CLIENT -> SERVER
 * Messages sent by the player to interact with the lobby.
 */
@Serializable
sealed class LobbyIntent : LobbyMessage() {

    @Serializable
    data class LeaveLobby(
        override val senderId: String? = null
    ) : LobbyIntent()

    @Serializable
    data class ToggleReady(
        val isReady: Boolean,
        override val senderId: String? = null
    ) : LobbyIntent()

    @Serializable
    data class ChangeColor(
        val newColor: String,
        override val senderId: String? = null
    ) : LobbyIntent()

    @Serializable
    data class RequestStartGame(
        override val senderId: String? = null
    ) : LobbyIntent()
}