package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.users.verify.AccountVerificationService
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.usersRoutes(
    accountVerificationService: AccountVerificationService,
    //changePasswordService: ChangePasswordService,
) {
    route("/users") {

        post("/email/confirm") {
            val request = call.receive<VerifyEmailRequest>()
            val response = accountVerificationService.verifyEmail(request)
            call.respond(response.result.toHttpStatus(),response)
        }

        post("/email/resend") {
            val request = call.receive<ResendVerificationCodeRequest>()
            val response = accountVerificationService.resendVerificationCode(request)
            call.respond(response.result.toHttpStatus(), response)
        }

//        post("/me/password") {
//            val userId = call.principal<JWTPrincipal>()
//                ?.payload
//                ?.getClaim("id")
//                ?.asString()
//                ?: return@post call.respond(
//                    HttpStatusCode.Unauthorized,
//                    "Access token must be provided"
//                )
//
//            val request = call.receive<ChangePasswordRequest>()
//            val response = changePasswordService.changeWithOldPassword(userId, request)
//
//            call.respond(response.result.toHttpStatus(), response)
//        }
//
//        post("/password/reset") {
//            val request = call.receive<ResetPasswordRequest>()
//            val response = changePasswordService.resetPassword(request)
//            call.respond(response.result.toHttpStatus(), response)
//        }
//
//        delete("/me") {
//            val userId = call.principal<JWTPrincipal>()
//                ?.payload
//                ?.getClaim("id")
//                ?.asString()
//                ?: return@delete call.respond(
//                    HttpStatusCode.Unauthorized,
//                    "Access token must be provided"
//                )
//
//            val request = call.receive<ChangePasswordRequest>()
//            val response = changePasswordService.changeWithOldPassword(userId, request)
//
//            call.respond(response.result.toHttpStatus(), response)
//        }
    }
}
