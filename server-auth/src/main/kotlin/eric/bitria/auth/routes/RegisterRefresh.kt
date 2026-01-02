package eric.bitria.auth.routes

import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
//
//fun Route.refreshRoute(tokenService: TokenService) {
//    post("/auth/refresh") {
//        val request = call.receive<RefreshRequest>()
//        val response = RefreshResponse(
//            RefreshResult.SUCCESS,
//            "Success",
//            tokenService.generateAccessToken(),
//            tokenService.generateRefreshToken()
//        )
//        call.respond(response.result.toHttpStatus(),response)
//    }
//}