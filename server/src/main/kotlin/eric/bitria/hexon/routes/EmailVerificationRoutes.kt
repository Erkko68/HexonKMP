package eric.bitria.hexon.routes

import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.emailVerificationRoutes(
    smtpService: SmtpService
) {

    post("/email-verifications") {
        val request = call.receive<RegisterRequest>()
        val response = registerService.register(request)
        call.respond(response.result.toHttpStatus(),response)
    }

}