package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.matchmaking.CreateLobbyRequest
import eric.bitria.hexon.dtos.matchmaking.JoinGameRequest
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.matchmaking.LobbyService
import eric.bitria.hexon.services.matchmaking.MatchmakingService
import eric.bitria.hexon.utils.toHttpStatus
import eric.bitria.hexon.ws.GameMessage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
            val response = matchMakingService.findGameForPlayer(userId = userId, mode = request.mode, maxPlayers = 1)
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

        val gameJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type" // Ensures JSON has {"type": "LobbyIntent.ChangeColor"}
        }

        webSocket("/game/{sessionId}") {
            val sessionId = call.parameters["sessionId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing ID"))

            // JWT Authentication
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.subject ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            val username = principal.payload.getClaim("username").asString() ?: "Unknown"

            // 1. Retrieve Session
            val session = gameSessionRepository.getSession(sessionId)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session invalid"))

            // 2. Handshake / Connect
            val connected = session.connectPlayer(userId, username, this)
            if (!connected) {
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room full or not reserved"))
            }

            try {
                // 3. Message Loop
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()

                        try {
                            // Deserialize
                            val message = gameJson.decodeFromString<GameMessage>(text)

                            // Delegate to Session
                            session.handleIncomingMessage(userId, message)

                        } catch (e: SerializationException) {
                            // 1. Client sent invalid JSON or unknown message type.
                            application.log.warn("Invalid JSON from user $username: ${e.message}")
                        } catch (e: Exception) {
                            // 2. Unexpected Server Logic Error (Crash in handleIncomingMessage).
                            application.log.error("Error processing message from $username: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                // 3. Transport Error (e.g. Network failure affecting the loop itself)
                application.log.error("WebSocket connection failure for $username: ${e.message}")
            } finally {
                // 4. Cleanup
                session.removePlayer(userId)
            }
        }
    }
}
