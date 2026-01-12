package eric.bitria.hexon.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.socialRoutes() {
    // Inject your business logic service
    val socialService: SocialService by inject()

    route("/social") {
        // Protect all these routes with JWT
        authenticate() {

            // 1. Get Friends List
            get("/friends") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val response = socialService.getFriends(userId)
                call.respond(response)
            }

            // 2. Send Friend Request
            post("/friends/add") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<AddFriendRequest>()

                // Logic to add friend
                val response = socialService.sendFriendRequest(
                    requesterId = userId,
                    targetUsername = request.targetUsername
                )

                call.respond(response)
            }

            // 3. Accept or Decline Request
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

                call.respond(response)
            }
        }
    }
}