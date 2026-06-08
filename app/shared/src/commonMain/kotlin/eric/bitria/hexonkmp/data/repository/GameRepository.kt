package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    val events: Flow<CatanServerEvent>

    // The id and game we last connected with, set by connect(). The in-game
    // ViewModel reads these at the lobby->game handoff (it isn't the one that joined).
    val currentPlayerId: String?
    val currentGameId: String?

    // The most recent GameStarted snapshot, cached as it passes through the event
    // stream. This is the seam between the two screens: the LobbyViewModel navigates
    // on GameStarted, then the GameViewModel — created fresh on the game route, after
    // the event already fired — seeds its initial state from here rather than racing
    // to catch the event. Safe because the game opens in Setup and the server then
    // waits for player actions (no updates arrive before the game screen mounts).
    val startedGame: GameState?

    suspend fun register(name: String, existingPlayerId: String?): RegisterResponse
    suspend fun joinGame(playerId: String): JoinGameResponse
    fun connect(playerId: String, gameId: String)
    fun sendAction(action: GameAction)
    fun disconnect()
}
