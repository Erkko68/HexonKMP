package eric.bitria.auth.routes

import eric.bitria.auth.register.RegisterService
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * Defines the routes for registration, email verification, and resending codes.
 */
fun Route.registerRoutes(registerService: RegisterService) {

    post("/auth/register") {
        val request = call.receive<RegisterRequest>()
        val response = registerService.register(request)
        call.respond(response.result.toHttpStatus(),response)
    }

    post("/auth/verify") {
        val request = call.receive<VerifyEmailRequest>()
        val response = registerService.verifyEmail(request.email, request.verificationCode)
        call.respond(response.result.toHttpStatus(),response)
    }

    post("/auth/resend-verification") {
        val request = call.receive<ResendVerificationCodeRequest>()
        val response = registerService.resendVerificationCode(request)
        call.respond(response.result.toHttpStatus(),response)
    }
}