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
import org.koin.ktor.ext.inject

fun Route.socialRoutes() {
    val socialService by inject<SocialService>()

    authenticate {

        // 1. Get Friends
        get("/friends") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val response = socialService.getFriends(userId)
            call.respond(response.result.toHttpStatus(), response)
        }

        // 2. Add Friend
        post("/friends/add") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val request = call.receive<AddFriendRequest>()
            val response = socialService.sendFriendRequest(userId, request.targetUsername)

            call.respond(response.result.toHttpStatus(), response)
        }

        // 3. Respond (Accept/Decline)
        post("/friends/respond") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

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
