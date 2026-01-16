package eric.bitria.hexon.dtos.matchmaking

import kotlinx.serialization.Serializable

@Serializable
sealed class GameMessage {

    /** Server to Client: Update on lobby status */
    @Serializable
    data class LobbyUpdate(
        val players: Int,
        val maxPlayers: Int,
        val isReady: Boolean
    ) : GameMessage()

    /** Server to Client: Game has started */
    @Serializable
    data class GameStarted(
        val sessionId: String,
        val initialState: String // Serialized game state
    ) : GameMessage()

    /** Server to Client: General game state update */
    @Serializable
    data class GameUpdate(
        val state: String
    ) : GameMessage()

    /** Client to Server: Player makes a move */
    @Serializable
    data class PlayerMove(
        val moveData: String
    ) : GameMessage()

    /** Server to Client: Error notification */
    @Serializable
    data class Error(
        val message: String
    ) : GameMessage()
}
