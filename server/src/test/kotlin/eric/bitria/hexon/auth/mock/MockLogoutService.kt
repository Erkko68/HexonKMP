package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResponse
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.services.auth.logout.LogoutService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.utils.TokenHasher

class MockLogoutService(
    private val authRepository: AuthRepository,
    private val tokenService: TokenService
) : LogoutService {

    override suspend fun logout(refreshToken: String, request: LogoutRequest): LogoutResponse {
        val tokenHash = TokenHasher.hash(refreshToken)

        if (request.logoutAllDevices == true) {
            val userId = tokenService.validateRefreshToken(refreshToken)
            if (userId != null) {
                authRepository.revokeAllRefreshTokens(userId)
            } else {
                authRepository.revokeRefreshToken(tokenHash)
            }
        } else {
            authRepository.revokeRefreshToken(tokenHash)
        }

        return LogoutResponse(LogoutResult.SUCCESS, "Logged out successfully")
    }
}
