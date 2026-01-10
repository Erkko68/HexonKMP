package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.users.verify.AccountVerificationService
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.users.account.UserAccountService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.usersRoutes(
    accountVerificationService: AccountVerificationService,
    userAccountService: UserAccountService
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

        authenticate {

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
                val response = userAccountService.changePassword(userId, request)

                call.respond(response.result.toHttpStatus(), response)
            }
        }

        post("/password/forgot") {
            val request = call.receive<ForgotPasswordRequest>()
            val response = userAccountService.forgotPassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        // Step 2: Confirm Reset
        post("/password/reset") {
            val request = call.receive<ResetPasswordRequest>()
            val response = userAccountService.resetPassword(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        authenticate {
            // Step 1: Initiate Deletion (Send Email)
            post("/me/delete/initiate") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject ?: return@post

                val response = userAccountService.requestAccountDeletion(userId)
                call.respond(HttpStatusCode.OK, response)
            }

            // Step 2: Confirm Deletion (Execute)
            delete("/me") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject ?: return@delete

                val request = call.receive<ConfirmDeleteAccountRequest>()
                val response = userAccountService.confirmAccountDeletion(userId, request)

                call.respond(response.result.toHttpStatus(), response)
            }
        }
    }
}
