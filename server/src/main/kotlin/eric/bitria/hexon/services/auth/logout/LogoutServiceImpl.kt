package eric.bitria.hexon.services.auth.logout

import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResponse
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.utils.TokenHasher

class LogoutServiceImpl(
    private val authRepository : AuthRepository,
    private val tokenService : TokenService
) : LogoutService {

    override suspend fun logout(request: LogoutRequest): LogoutResponse {
        return try {
            // 1. Hash the incoming token so we can find it in the DB
            val tokenHash = TokenHasher.hash(request.refreshToken)

            // Does this session actually exist?
            // If the token is invalid, forged, or already revoked, we stop here.
            if (!authRepository.hasRefreshTokenHash(tokenHash)) {
                // If it's not in the DB, the user is effectively already logged out.
                // We return SUCCESS so the client clears the cookie.
                return LogoutResponse(LogoutResult.SUCCESS, "Session already closed")
            }

            if (request.logoutAllDevices == true) {
                // --- LOGOUT ALL DEVICES ---

                // 3. Extract User ID from the JWT
                // Since we verified the token exists in DB, we know it's one of OUR valid tokens.
                val userId = tokenService.validateRefreshToken(request.refreshToken)

                if (userId != null) {
                    authRepository.revokeAllRefreshTokens(userId)
                    LogoutResponse(LogoutResult.SUCCESS, "Logged out from all devices")
                } else {
                    // Token existed in DB but failed validation (e.g. expired just now)?
                    // Fallback: just revoke this specific one.
                    authRepository.revokeRefreshToken(tokenHash)
                    LogoutResponse(LogoutResult.SUCCESS, "Logged out (Token invalid)")
                }

            } else {
                // --- LOGOUT CURRENT DEVICE ONLY ---
                authRepository.revokeRefreshToken(tokenHash)
                LogoutResponse(LogoutResult.SUCCESS, "Logged out successfully")
            }

        } catch (e: Exception) {
            LogoutResponse(LogoutResult.UNKNOWN_ERROR, "Internal server error")
        }
    }
}