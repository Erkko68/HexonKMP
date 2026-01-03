package eric.bitria.auth.routes

import eric.bitria.auth.refresh.RefreshService
import eric.bitria.auth.utils.toHttpStatus
import eric.bitria.hexon.dtos.auth.RefreshRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.refreshRoute(refreshService: RefreshService) {
    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        val response = refreshService.refresh(request)
        call.respond(response.result.toHttpStatus(), response)
    }
}