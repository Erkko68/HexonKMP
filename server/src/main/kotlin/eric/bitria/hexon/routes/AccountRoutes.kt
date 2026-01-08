package eric.bitria.hexon.routes

import eric.bitria.hexon.account.password.PasswordService
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.accountRoutes(passwordService: PasswordService) {
    route("/account") {
        post("/change-password") {
            val request = call.receive<ChangePasswordRequest>()
            val response = passwordService.changePassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            val response = passwordService.forgotPasswordCodeRequest(request)
            call.respond(response.result.toHttpStatus(), response)
        }


    }
}
