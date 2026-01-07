package eric.bitria.hexon.routes

import eric.bitria.hexon.auth.password.PasswordService
import eric.bitria.hexon.dtos.auth.ChangePasswordRequest
import eric.bitria.hexon.dtos.auth.ForgotPasswordRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.passwordRoutes(passwordService: PasswordService) {

    post("/auth/change-password") {
        val request = call.receive<ChangePasswordRequest>()
        val response = passwordService.changePassword(request)
        call.respond(response.result.toHttpStatus(),response)
    }

    post("/auth/forgot-password") {
        val request = call.receive<ForgotPasswordRequest>()
        val response = passwordService.forgotPassword(request)
        call.respond(response.result.toHttpStatus(),response)
    }

}