package eric.bitria.hexon.ws

import kotlinx.serialization.Serializable
/**
 * CLIENT -> SERVER
 * Messages sent by the player to interact with the lobby.
 */
@Serializable
sealed class LobbyIntent : LobbyMessage() {

    @Serializable
    data object LeaveLobby: LobbyIntent()

    @Serializable
    data class ToggleReady(
        val isReady: Boolean,
    ) : LobbyIntent()

    @Serializable
    data class ChangeColor(
        val newColor: String,
    ) : LobbyIntent()

    @Serializable
    data object RequestStartGame : LobbyIntent()

    /** Client signals it received GameStarted and is ready for game initialization */
    @Serializable
    data object ReadyForGame : LobbyIntent()
}