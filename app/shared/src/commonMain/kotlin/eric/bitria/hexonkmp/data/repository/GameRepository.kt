package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.CreateLobbyResponse
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.JoinLobbyResponse
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

    // The public roster (playerId -> display name) from the latest GameStarted, for
    // showing opponents by name in-game. Travels with the snapshot at the handoff.
    val startedNames: Map<String, String>

    // register is the auth/refresh endpoint (token in the body). The other HTTP calls
    // authenticate via the client's Auth bearer header, so they take no token.
    suspend fun register(name: String, token: String?): RegisterResponse
    suspend fun joinGame(): JoinGameResponse

    // Private lobbies.
    suspend fun createLobby(): CreateLobbyResponse
    suspend fun joinLobby(code: String): JoinLobbyResponse
    suspend fun startLobby(gameId: String)

    // The WS authenticates with [token] (query param); [playerId] is the caller's own
    // id (known from registration) kept for local bookkeeping, and [name] feeds the roster.
    fun connect(token: String, playerId: String, name: String, gameId: String)
    fun sendAction(action: GameAction)
    fun disconnect()
}
