package eric.bitria.hexon.mock

import eric.bitria.hexon.auth.refresh.RefreshService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult

class MockRefreshService(private val tokenService: TokenService) : RefreshService {
    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val userId = tokenService.verifyToken(request.refreshToken)
            ?: return RefreshResponse(
                result = RefreshResult.INVALID_TOKEN,
                message = "Invalid or expired refresh token",
                accessToken = "",
                refreshToken = ""
            )

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = tokenService.generateAccessToken(userId),
            refreshToken = tokenService.generateRefreshToken(userId)
        )
    }
}
