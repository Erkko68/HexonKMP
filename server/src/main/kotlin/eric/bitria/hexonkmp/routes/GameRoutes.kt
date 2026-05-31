package eric.bitria.hexonkmp.routes

import eric.bitria.hexonkmp.core.protocol.JoinGameRequest
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.Wire
import eric.bitria.hexonkmp.repository.GameSessionRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject

fun Application.gameRoutes() {
    val sessions by inject<GameSessionRepository>()

    routing {
        // POST /game — client sends its stable playerId; server finds or creates a game slot.
        post("/game") {
            val request = call.receive<JoinGameRequest>()
            val session = sessions.findOrJoin(request.playerId)
            call.respond(JoinGameResponse(playerId = request.playerId, gameId = session.gameId))
        }

        // WS /game/{gameId} — connect to an active session
        webSocket("/game/{gameId}") {
            val gameId = call.parameters["gameId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing gameId"))
            val playerId = call.request.queryParameters["playerId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing playerId"))

            val session = sessions.get(gameId)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.NORMAL, "Game not found"))

            if (!session.connect(playerId, this)) {
                return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No reservation for this player"))
            }

            try {
                // Decode client actions and hand them to the session; all game
                // logic lives behind the engine, this loop is pure transport.
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        runCatching { Wire.decodeAction(frame.readText()) }
                            .onSuccess { session.handleAction(playerId, it) }
                    }
                }
            } finally {
                session.disconnect(playerId)
            }
        }
    }
}
