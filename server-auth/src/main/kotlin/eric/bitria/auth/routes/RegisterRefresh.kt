package eric.bitria.auth.routes

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