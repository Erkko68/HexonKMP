package eric.bitria.hexon.ws.lobby

import kotlinx.serialization.Serializable

@Serializable
data class LobbyPlayer(
    val id: String,
    val name: String,
    val color: String,
    val isReady: Boolean,
    val isHost: Boolean
)
