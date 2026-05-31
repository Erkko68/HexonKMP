package eric.bitria.hexonkmp.routes

import eric.bitria.hexonkmp.core.dto.ErrorResponse
import eric.bitria.hexonkmp.core.dto.JoinGameResponse
import eric.bitria.hexonkmp.repository.GameSessionRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Application.gameRoutes() {
    val sessions by inject<GameSessionRepository>()

    routing {
        // POST /game — request to join a game; returns playerId + gameId
        post("/game") {
            val playerId = UUID.randomUUID().toString()
            val session = sessions.findOrJoin(playerId)
            call.respond(JoinGameResponse(playerId = playerId, gameId = session.gameId))
        }

        // WS /game/{gameId} — connect to an active session
        webSocket("/game/{gameId}") {
            val gameId = call.parameters["gameId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing gameId"))
            val playerId = call.request.headers["X-Player-Id"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing X-Player-Id header"))

            val session = sessions.get(gameId)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.NORMAL, "Game not found"))

            if (!session.connect(playerId, this)) {
                return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No reservation for this player"))
            }

            try {
                for (frame in incoming) {
                    // Intentionally empty — game logic handled in future iterations
                }
            } finally {
                session.disconnect(playerId)
            }
        }
    }
}
