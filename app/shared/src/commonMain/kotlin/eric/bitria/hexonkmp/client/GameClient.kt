package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.dto.JoinGameRequest
import eric.bitria.hexonkmp.core.dto.JoinGameResponse
import eric.bitria.hexonkmp.core.ws.ServerEvent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class GameClient(private val http: HttpClient) {

    suspend fun joinGame(playerId: String): JoinGameResponse =
        http.post("/game") { setBody(JoinGameRequest(playerId)) }.body()

    suspend fun connectToGame(
        playerId: String,
        gameId: String,
        onEvent: suspend (ServerEvent) -> Unit,
    ) {
        http.webSocket(path = "/game/$gameId", request = { header("X-Player-Id", playerId) }) {
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        runCatching { AppJson.decodeFromString<ServerEvent>(frame.readText()) }
                            .onSuccess { onEvent(it) }
                    }
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Player left"))
                }
                throw e
            }
        }
    }
}
