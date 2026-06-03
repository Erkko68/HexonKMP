package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.protocol.CatanCodec
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.JoinGameRequest
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameClient(private val http: HttpClient) {

    suspend fun joinGame(playerId: String): JoinGameResponse =
        http.post("/game") { setBody(JoinGameRequest(playerId)) }.body()

    suspend fun connectToGame(
        playerId: String,
        gameId: String,
        outgoing: Flow<GameAction>,
        onEvent: suspend (CatanServerEvent) -> Unit,
    ) {
        // playerId goes in the query string, not a header: browsers can't set
        // custom headers on WebSocket connections.
        http.webSocket(path = "/game/$gameId", request = { parameter("playerId", playerId) }) {
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
