package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.protocol.CatanCodec
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.CreateLobbyResponse
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

// HTTP auth (token attach + 401 refresh-and-retry) is handled centrally by the
// client's Auth bearer plugin (see HttpClientFactory), so these calls carry no token:
// register is the refresh endpoint (token in the body), everything else relies on the
// plugin's Authorization header. Only the WebSocket passes the token explicitly,
// because browsers can't set headers on the WS handshake.
class GameClient(private val http: HttpClient) {

    // Identity handshake / token refresh: send our chosen name (and stored token, if
    // any) and get back the server-issued playerId + token.
    suspend fun register(name: String, token: String?): RegisterResponse =
        http.post("/register") { setBody(RegisterRequest(name, token)) }.body()

    suspend fun joinGame(): JoinGameResponse =
        http.post("/game").body()

    // Create a private lobby (caller becomes host); returns the gameId + join code.
    suspend fun createLobby(): CreateLobbyResponse =
        http.post("/lobby").body()

    // Join a private lobby by code; returns the gameId. A non-success status (an
    // unknown/full code) throws so the caller can show "lobby not found".
    suspend fun joinLobby(code: String): JoinLobbyResponse {
        val response = http.post("/lobby/join") { setBody(JoinLobbyRequest(code)) }
        if (!response.status.isSuccess()) error("Lobby not found")
        return response.body()
    }

    // Host-only: start a private lobby.
    suspend fun startLobby(gameId: String) {
        http.post("/lobby/start") { setBody(StartLobbyRequest(gameId)) }
    }

    suspend fun connectToGame(
        token: String,
        name: String,
        gameId: String,
        outgoing: Flow<GameAction>,
        onEvent: suspend (CatanServerEvent) -> Unit,
    ) {
        // token + name go in the query string, not headers: browsers can't set
        // custom headers on WebSocket connections. The token authenticates the player.
        http.webSocket(
            path = "/game/$gameId",
            request = {
                parameter("token", token)
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
