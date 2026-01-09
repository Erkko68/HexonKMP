package eric.bitria.hexon.auth.refresh

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult

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
            result = RefreshResult.INVALID_TOKEN,
            message = "Invalid or expired refresh token",
            accessToken = "",
            refreshToken = ""
        )

        val result = BCrypt.verifyer().verify(request.refreshToken.toCharArray(), storedHash)
        if (!result.verified) {
            return RefreshResponse(
                result = RefreshResult.INVALID_TOKEN,
                message = "Invalid or expired refresh token",
                accessToken = "",
                refreshToken = ""
            )
        }

        val newAccessToken = tokenService.generateAccessToken(userId)
        val newRefreshToken = tokenService.generateRefreshToken(userId)
        
        val newRefreshTokenHash = BCrypt.withDefaults().hashToString(12, newRefreshToken.toCharArray())
        repository.updateRefreshTokenHash(userId, newRefreshTokenHash)

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}
