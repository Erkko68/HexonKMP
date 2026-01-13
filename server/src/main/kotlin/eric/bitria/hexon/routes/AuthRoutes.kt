package eric.bitria.hexon.routes

import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Defines the routes for registration, email verification, and resending codes.
 */
fun Route.authRoutes() {
    val registerService by inject<RegisterService>()
    val loginService by inject<LoginService>()
    val refreshService by inject<RefreshService>()

    route("/auth"){
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = registerService.register(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = loginService.login(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val response = refreshService.refresh(request)
            call.respond(response.result.toHttpStatus(), response)
        }
    }
}
