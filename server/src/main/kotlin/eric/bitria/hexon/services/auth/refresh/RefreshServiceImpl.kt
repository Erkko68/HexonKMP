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
        val userId =
            tokenService.validateRefreshToken(request.refreshToken) ?: return RefreshResponse(
                RefreshResult.INVALID_TOKEN,
                "Invalid or expired refresh token"
            )

        // 2. Fetch User
        val user = authRepository.findUserById(userId)
            ?: return RefreshResponse(RefreshResult.USER_NOT_FOUND, "User no longer exists.")

        // 3. Verify Token against DB Sessions
        // Calculate hash of the incoming token to see if this specific session is still active
        val incomingHash = TokenHasher.hash(request.refreshToken)
        val sessionExists = authRepository.hasRefreshTokenHash(incomingHash)
        
        if (!sessionExists) {
            // If the hash is not in DB, it means the session was revoked or this is a token reuse attempt.
            return RefreshResponse(RefreshResult.INVALID_TOKEN, "Session is no longer valid.")
        }

        // 4. Rotate Tokens
        val newAccessToken = tokenService.generateAccessToken(user.id, user.username)
        val newRefreshToken = tokenService.generateRefreshToken(user.id)

        // 5. Update DB (Replace old hash with new hash)
        val newRefreshTokenHash = TokenHasher.hash(newRefreshToken)
        val updated = authRepository.updateRefreshToken(incomingHash, newRefreshTokenHash)

        if (!updated) {
             return RefreshResponse(RefreshResult.UNKNOWN_ERROR, "Failed to rotate session.")
        }

        // 6. Return Success
        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Token refreshed successfully",
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}