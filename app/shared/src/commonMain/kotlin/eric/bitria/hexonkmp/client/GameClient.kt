package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.protocol.CatanCodec
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.CreateLobbyRequest
import eric.bitria.hexonkmp.core.protocol.CreateLobbyResponse
import eric.bitria.hexonkmp.core.protocol.JoinGameRequest
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.JoinLobbyRequest
import eric.bitria.hexonkmp.core.protocol.JoinLobbyResponse
import eric.bitria.hexonkmp.core.protocol.RegisterRequest
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import eric.bitria.hexonkmp.core.protocol.StartLobbyRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameClient(private val http: HttpClient) {

    // Identity handshake: send our chosen name (and prior id, if any) and get back
    // the server-issued playerId. Separate from joinGame so it can later become auth.
    suspend fun register(name: String, existingPlayerId: String?): RegisterResponse =
        http.post("/register") { setBody(RegisterRequest(name, existingPlayerId)) }.body()

    suspend fun joinGame(playerId: String): JoinGameResponse =
        http.post("/game") { setBody(JoinGameRequest(playerId)) }.body()

    // Create a private lobby (caller becomes host); returns the gameId + join code.
    suspend fun createLobby(playerId: String): CreateLobbyResponse =
        http.post("/lobby") { setBody(CreateLobbyRequest(playerId)) }.body()

    // Join a private lobby by code; returns the gameId to connect to. Throws on an
    // unknown/full code (non-2xx) so callers get a clean failure rather than a
    // confusing body-deserialization error.
    suspend fun joinLobby(code: String, playerId: String): JoinLobbyResponse {
        val response = http.post("/lobby/join") { setBody(JoinLobbyRequest(code, playerId)) }
        if (!response.status.isSuccess()) error("Lobby not found")
        return response.body()
    }

    // Host-only: start a private lobby.
    suspend fun startLobby(gameId: String, playerId: String) {
        http.post("/lobby/start") { setBody(StartLobbyRequest(gameId, playerId)) }
    }

    suspend fun connectToGame(
        playerId: String,
        name: String,
        gameId: String,
        outgoing: Flow<GameAction>,
        onEvent: suspend (CatanServerEvent) -> Unit,
    ) {
        // playerId + name go in the query string, not headers: browsers can't set
        // custom headers on WebSocket connections.
        http.webSocket(
            path = "/game/$gameId",
            request = {
                parameter("playerId", playerId)
                parameter("name", name)
            },
        ) {
            coroutineScope {
                // Pump outbound actions in a child coroutine of this session.
                val sender = launch {
                    outgoing.collect { action ->
                        send(Frame.Text(CatanCodec.encodeAction(action)))
                    }
                }
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            runCatching { CatanCodec.decodeServerEvent(frame.readText()) }
                                .onSuccess { onEvent(it) }
                        }
                    }
                } catch (e: CancellationException) {
                    withContext(NonCancellable) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Player left"))
                    }
                    throw e
                } finally {
                    sender.cancel()
                }
            }
        }
    }
}
