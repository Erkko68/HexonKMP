package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.matchmaking.CreateLobbyRequest
import eric.bitria.hexon.dtos.matchmaking.JoinGameRequest
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.matchmaking.LobbyService
import eric.bitria.hexon.services.matchmaking.MatchmakingService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import org.koin.ktor.ext.inject

fun Route.matchmakingRoutes(){

    val matchMakingService by inject<MatchmakingService>()
    val gameSessionRepository by inject<GameSessionRepository>()
    val lobbyService by inject<LobbyService>()

    authenticate {
        post("/game") {
            val userId = call.principal<JWTPrincipal>()?.payload?.subject
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val request = call.receive<JoinGameRequest>()
            val response = matchMakingService.findGameForPlayer(userId = userId, mode = request.mode)
            call.respond(response.status.toHttpStatus(), response)
        }

        post("/lobby") {
            val creatorId = call.principal<JWTPrincipal>()?.payload?.subject
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val request = call.receive<CreateLobbyRequest>()
            val response = lobbyService.createCustomGame(
                creatorId = creatorId,
                mode = request.mode,
                maxPlayers = request.maxPlayers,
            )

            call.respond(response.status.toHttpStatus(), response)
        }

        webSocket("/game/{sessionId}") {
            val sessionId = call.parameters["sessionId"]
                ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "Missing sessionId"
                    )
                )

            val userId = call.principal<JWTPrincipal>()?.payload?.subject
                ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Unauthorized"
                    )
                )

            val session = gameSessionRepository.getSession(sessionId)
                ?: return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "Invalid sessionId"
                    )
                )

            val connected = session.connectPlayer(userId, this)
            if (!connected) {
                return@webSocket close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "Cannot connect, slot not reserved"
                    )
                )
            }

            try {
                // Broadcast lobby updates automatically
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        //session.currentEngine?.applyMove(userId, parseMove(text))
                    }
                }
            } finally {
                session.removePlayer(userId)
            }
        }
    }
}