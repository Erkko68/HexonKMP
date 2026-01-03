package eric.bitria.auth.refresh

import eric.bitria.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult

class RefreshServiceImp(private val tokenService: TokenService) : RefreshService {
    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val userId = tokenService.verifyToken(request.refreshToken)
            ?: return RefreshResponse(
                result = RefreshResult.INVALID_TOKEN,
                message = "Invalid or expired refresh token",
                accessToken = "",
                refreshToken = ""
            )

        val newAccessToken = tokenService.generateAccessToken(userId)
        val newRefreshToken = tokenService.generateRefreshToken(userId)

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}
