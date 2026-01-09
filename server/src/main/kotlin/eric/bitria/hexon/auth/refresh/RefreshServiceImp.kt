package eric.bitria.hexon.auth.refresh

import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.utils.hashToken

class RefreshServiceImp(
    private val repository: AuthRepository,
    private val tokenService: TokenService
) : RefreshService {
    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val userId = tokenService.verifyToken(request.refreshToken)
            ?: return RefreshResponse(
                result = RefreshResult.INVALID_TOKEN,
                message = "Invalid or expired refresh token",
                accessToken = "",
                refreshToken = ""
            )

        val storedHash = repository.getRefreshTokenHash(userId) ?: return RefreshResponse(
            result = RefreshResult.UNKNOWN_ERROR,
            message = "User should have a refresh token but it was null",
            accessToken = "",
            refreshToken = ""
        )

        val currentTokenHash = hashToken(request.refreshToken)
        if (currentTokenHash != storedHash) {
            return RefreshResponse(
                result = RefreshResult.INVALID_TOKEN,
                message = "Invalid or expired refresh token",
                accessToken = "",
                refreshToken = ""
            )
        }

        val newAccessToken = tokenService.generateAccessToken(userId)
        val newRefreshToken = tokenService.generateRefreshToken(userId)

        val refreshTokenHash = hashToken(newRefreshToken)
        repository.updateRefreshTokenHash(userId, refreshTokenHash)

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}