package eric.bitria.hexon.routes

import eric.bitria.hexon.account.password.ChangePasswordService
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.accountRoutes(changePasswordService: ChangePasswordService) {
    route("/account") {

        post("/password") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload
                ?.getClaim("id")
                ?.asString()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    "Access token must be provided"
                )

            val request = call.receive<ChangePasswordRequest>()
            val response = changePasswordService.changeWithOldPassword(userId, request)

            call.respond(response.result.toHttpStatus(), response)
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            val response = changePasswordService.resetPassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }
    }
}
