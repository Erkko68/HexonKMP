package eric.bitria.hexon.ws

import eric.bitria.hexon.ws.lobby.LobbyPlayer
import kotlinx.serialization.Serializable

/**
 * SERVER -> CLIENT
 * Updates sent to all clients to keep their UI in sync.
 */
@Serializable
sealed class LobbyEvent : LobbyMessage() {

    /** * Full Snapshot: Sent immediately when a player joins
     * so they know the current state of everything.
     */
    @Serializable
    data class LobbySnapshot(
        val lobbyId: String,
        val lobbyPlayers: List<LobbyPlayer>,
        val maxPlayers: Int,
        val availableColors: List<String>,
        override val senderId: String? = "Server"
    ) : LobbyEvent()

    /** Incremental Update: A new player entered */
    @Serializable
    data class PlayerJoined(
        val lobbyPlayer: LobbyPlayer,
        override val senderId: String? = "Server"
    ) : LobbyEvent()

    /** Incremental Update: A player left */
    @Serializable
    data class PlayerLeft(
        val playerId: String,
        override val senderId: String? = "Server"
    ) : LobbyEvent()

    /** Incremental Update: Someone changed ready status, color, etc. */
    @Serializable
    data class PlayerUpdated(
        val lobbyPlayer: LobbyPlayer,
        override val senderId: String? = "Server"
    ) : LobbyEvent()

    /** * Transition: The host started the game.
     * Clients should now switch scenes to the Game Board.
     */
    @Serializable
    data class GameStarted(
        val initialTurnPlayerId: String,
        override val senderId: String? = "Server"
    ) : LobbyEvent()

    /** specific errors related to lobby (e.g., "Color already taken") */
    @Serializable
    data class LobbyError(
        val errorMessage: String,
        val code: LobbyErrorCode,
        override val senderId: String? = "Server"
    ) : LobbyEvent()
}

enum class LobbyErrorCode{
    COLOR_TAKEN
}