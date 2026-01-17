package eric.bitria.hexon.ws.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val color: String,
    val isReady: Boolean,
    val isHost: Boolean
)
