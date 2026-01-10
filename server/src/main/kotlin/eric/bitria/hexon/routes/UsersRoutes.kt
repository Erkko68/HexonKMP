package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.users.verify.AccountVerificationService
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.users.password.PasswordService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.usersRoutes(
    accountVerificationService: AccountVerificationService,
    passwordService: PasswordService
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

        post("/password/change") {
            // 1. Extract User ID from the Token Subject
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.subject

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token claims")
                return@post
            }

            // 2. Process Request
            val request = call.receive<ChangePasswordRequest>()
            val response = passwordService.changePassword(userId, request)

            call.respond(response.result.toHttpStatus(), response)
        }

        post("/password/forgot") {
            val request = call.receive<ForgotPasswordRequest>()
            val response = passwordService.forgotPassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        // Step 2: Confirm Reset
        post("/password/reset") {
            val request = call.receive<ResetPasswordRequest>()
            val response = passwordService.resetPassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }
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
