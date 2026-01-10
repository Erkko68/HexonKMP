package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.refresh.RefreshService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.utils.TokenHasher

class MockRefreshService(
    private val repository: AuthRepository,
    private val tokenService: TokenService
) : RefreshService {
    
    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        // 1. Validate Token Structure
        val userId = tokenService.validateRefreshToken(request.refreshToken)
            ?: return RefreshResponse(RefreshResult.INVALID_TOKEN, "Invalid token")

        // 2. Fetch User
        val user = repository.findUserById(userId)
            ?: return RefreshResponse(RefreshResult.USER_NOT_FOUND, "User not found")

        // 3. Check Stored Hash (Simulation)
        val storedHash = repository.getRefreshTokenHash(userId)
            ?: return RefreshResponse(RefreshResult.INVALID_TOKEN, "No active session")

        // In the mock, we simulate hashing by just comparing strings or using the utility if available
        // To be truly stateful, we check if the token matches the "last issued" one
        val isMatch = TokenHasher.verify(request.refreshToken, storedHash)
        
        if (!isMatch) {
            // Simulate Token Reuse Detection: Revoke all tokens
            repository.updateRefreshToken(userId, null)
            return RefreshResponse(RefreshResult.TOKEN_MISMATCH, "Token reuse detected")
        }

        // 4. Generate New Tokens
        val newAccess = tokenService.generateAccessToken(userId, user.email)
        val newRefresh = tokenService.generateRefreshToken(userId)

        // 5. Update State
        repository.updateRefreshToken(userId, TokenHasher.hash(newRefresh))

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Refreshed",
            accessToken = newAccess,
            refreshToken = newRefresh
        )
    }
}
