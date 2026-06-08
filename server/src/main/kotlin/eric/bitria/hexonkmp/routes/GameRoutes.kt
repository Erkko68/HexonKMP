package eric.bitria.hexonkmp.routes

import eric.bitria.hexonkmp.core.protocol.JoinGameRequest
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.RegisterRequest
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import eric.bitria.hexonkmp.repository.GameSessionRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Application.gameRoutes() {
    val sessions by inject<GameSessionRepository<*, *, *>>()

    routing {
        // POST /register — identity handshake, separate from matchmaking. The server
        // is authoritative over player ids: it reuses a client-supplied id only if
        // present (reconnection before real auth exists), otherwise it mints one.
        // The name is echoed back for now; it's the field a future auth flow grows from.
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val playerId = request.existingPlayerId?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()
            call.respond(RegisterResponse(playerId = playerId, name = request.name))
        }

        // POST /game — client sends its server-issued playerId; server finds or creates a slot.
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
                // Forward raw client action frames to the session, which decodes
                // them with its game codec; this loop is pure transport and knows
                // nothing about the game or its action types.
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        session.handleAction(playerId, frame.readText())
                    }
                }
            } finally {
                session.disconnect(playerId)
            }
        }
    }
}
