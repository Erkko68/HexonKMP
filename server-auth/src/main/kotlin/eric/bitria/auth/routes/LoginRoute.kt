package eric.bitria.auth.routes

import eric.bitria.auth.login.LoginService
import eric.bitria.auth.utils.toHttpStatus
import eric.bitria.hexon.dtos.auth.LoginRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.loginRoute(loginService: LoginService) {
    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val response = loginService.login(request)
        call.respond(response.result.toHttpStatus(), response)
    }
}