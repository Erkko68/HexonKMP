package eric.bitria.hexon.routes

import eric.bitria.hexon.auth.register.AccountVerificationService
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.emailVerificationRoutes(
    accountVerificationService: AccountVerificationService
) {
    post("/email-verification") {
        val request = call.receive<VerifyEmailRequest>()
        val response = accountVerificationService.verifyEmail(request)
        call.respond(response.result.toHttpStatus(),response)
    }
}