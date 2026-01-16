package eric.bitria.hexon.services.auth.refresh

import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.utils.TokenHasher

class RefreshServiceImpl(
    private val authRepository: AuthRepository,
    private val tokenService: TokenService
) : RefreshService {

    override suspend fun refresh(request: RefreshRequest): RefreshResponse {

        // 1. Validate JWT Structure & Expiry
        // Check if it's a valid JWT signed by us and not expired
        val userId =
            tokenService.validateRefreshToken(request.refreshToken) ?: return RefreshResponse(
                RefreshResult.INVALID_TOKEN,
                "Invalid or expired refresh token"
            )

        // 2. Fetch User & Stored Hash
        // We need to see if the user exists and grab the currently active session hash
        val user = authRepository.findUserById(userId)
            ?: return RefreshResponse(RefreshResult.USER_NOT_FOUND, "User no longer exists.")

        val storedHash = authRepository.getRefreshTokenHash(userId)
            ?: // User logged out explicitly, so the hash was cleared
            return RefreshResponse(RefreshResult.INVALID_TOKEN, "Session expired.")

        // 3. Verify Token against DB Hash
        // This prevents "Token Reuse". If the user (or attacker) tries to use
        // an old refresh token that we already rotated out, this check will fail.
        val isMatch = TokenHasher.verify(request.refreshToken, storedHash)
        if (!isMatch) {
            // SECURITY ALERT: Token Reuse Detected.
            authRepository.updateRefreshToken(userId, null)

            return RefreshResponse(RefreshResult.TOKEN_MISMATCH, "Invalid session token")
        }

        // 4. Rotate Tokens
        val newAccessToken = tokenService.generateAccessToken(user.id, user.username)
        val newRefreshToken = tokenService.generateRefreshToken(user.id)

        // 5. Update DB with New Hash
        val newRefreshTokenHash = TokenHasher.hash(newRefreshToken)

        authRepository.updateRefreshToken(user.id, newRefreshTokenHash)

        // 6. Return Success
        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}