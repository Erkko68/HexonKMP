package eric.bitria.hexon.routes

import eric.bitria.hexon.auth.login.LoginService
import eric.bitria.hexon.auth.refresh.RefreshService
import eric.bitria.hexon.auth.register.RegisterService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.utils.toHttpStatus
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.SendEmailVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * Defines the routes for registration, email verification, and resending codes.
 */
fun Route.authRoutes(
    registerService: RegisterService,
    loginService: LoginService,
    refreshService: RefreshService
) {

    post("/auth/register") {
        val request = call.receive<RegisterRequest>()
        val response = registerService.register(request)
        call.respond(response.result.toHttpStatus(),response)
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val response = loginService.login(request)
        call.respond(response.result.toHttpStatus(), response)
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        val response = refreshService.refresh(request)
        call.respond(response.result.toHttpStatus(), response)
    }

}