package eric.bitria.hexonkmp.routes

import eric.bitria.hexonkmp.core.protocol.CreateLobbyResponse
import eric.bitria.hexonkmp.core.protocol.ErrorResponse
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import eric.bitria.hexonkmp.core.protocol.JoinLobbyRequest
import eric.bitria.hexonkmp.core.protocol.JoinLobbyResponse
import eric.bitria.hexonkmp.core.protocol.RegisterRequest
import eric.bitria.hexonkmp.core.protocol.RegisterResponse
import eric.bitria.hexonkmp.core.protocol.StartLobbyRequest
import eric.bitria.hexonkmp.auth.TokenRegistry
import eric.bitria.hexonkmp.repository.GameSessionRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject

// The bearer token the client's Auth plugin attaches: "Authorization: Bearer <token>".
private fun ApplicationCall.bearerToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substring(7)
        ?.trim()

fun Application.gameRoutes() {
    val sessions by inject<GameSessionRepository<*, *, *>>()
    val tokens by inject<TokenRegistry>()

    // Resolve the caller's playerId from their bearer token, or respond 401.
    suspend fun RoutingContext.authedPlayerId(): String? {
        val playerId = call.bearerToken()?.let { tokens.resolve(it) }
        if (playerId == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
        return playerId
    }

    routing {
        // POST /register — anonymous device identity. Reuses the identity behind a
        // supplied token (reconnection / new session on the same device) or mints a
        // fresh playerId + token. The token is the secret the client stores and
        // presents (as a bearer header) on every later request. This is also the
        // client Auth plugin's refresh endpoint, so it carries its token in the body.
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val (playerId, token) = tokens.registerOrReuse(request.token)
            call.respond(RegisterResponse(playerId = playerId, name = request.name, token = token))
        }

        // POST /game — find or create a slot for the authenticated caller.
        post("/game") {
            val playerId = authedPlayerId() ?: return@post
            val session = sessions.findOrJoin(playerId)
            call.respond(JoinGameResponse(gameId = session.gameId))
        }

        // POST /lobby — create a private lobby; the caller becomes its host.
        post("/lobby") {
            val playerId = authedPlayerId() ?: return@post
            val (session, code) = sessions.createLobby(playerId)
            call.respond(CreateLobbyResponse(gameId = session.gameId, code = code))
        }

        // POST /lobby/join — resolve a join code and reserve the caller's seat.
        post("/lobby/join") {
            val playerId = authedPlayerId() ?: return@post
            val request = call.receive<JoinLobbyRequest>()
            val session = sessions.joinByCode(request.code, playerId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Lobby not found"))
            call.respond(JoinLobbyResponse(gameId = session.gameId))
        }

        // POST /lobby/start — host-only start of a private lobby.
        post("/lobby/start") {
            val playerId = authedPlayerId() ?: return@post
            val request = call.receive<StartLobbyRequest>()
            val started = sessions.get(request.gameId)?.startByHost(playerId) ?: false
            if (started) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.Conflict, ErrorResponse("Cannot start this lobby"))
        }

        // WS /game/{gameId} — connect to an active session. The token authenticates the
        // player (query param, since browsers can't set headers on WebSockets).
        webSocket("/game/{gameId}") {
            val gameId = call.parameters["gameId"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing gameId"))
            val token = call.request.queryParameters["token"]
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
            val playerId = tokens.resolve(token)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            // Display name for the lobby roster; absent on older clients, default to id.
            val name = call.request.queryParameters["name"] ?: playerId

            val session = sessions.get(gameId)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.NORMAL, "Game not found"))

            if (!session.connect(playerId, name, this)) {
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
