package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.services.social.SocialService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.socialRoutes() {
    val socialService by inject<SocialService>()

    authenticate {
        route("/friends") {
            // 1. Get Friends - GET /friends
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token claims")

                val response = socialService.getFriends(userId)
                call.respond(response.result.toHttpStatus(), response)
            }

            // 2. Get Friend Requests - GET /friends/requests
            get("/requests") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token claims")

                val response = socialService.getFriendRequests(userId)
                call.respond(response.result.toHttpStatus(), response)
            }

            // 3. Add Friend - POST /friends/add
            post("/add") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token claims")

                val request = call.receive<AddFriendRequest>()
                val response = socialService.sendFriendRequest(userId, request.targetUsername)

                call.respond(response.result.toHttpStatus(), response)
            }

            // 4. Respond (Accept/Decline) - POST /friends/respond
            post("/respond") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token claims")

                val request = call.receive<RespondFriendRequest>()
                val response = socialService.respondToRequest(
                    userId = userId,
                    requesterUsername = request.requesterUsername,
                    action = request.action
                )

                call.respond(response.result.toHttpStatus(), response)
            }
        }
    }
}
