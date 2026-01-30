package eric.bitria.hexon.auth.mock

import com.auth0.jwt.JWT
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.utils.TokenHasher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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

        // 3. Check Stored Hash
        val incomingHash = TokenHasher.hash(request.refreshToken)
        val hasSession = repository.hasRefreshTokenHash(incomingHash)
        
        if (!hasSession) {
            return RefreshResponse(RefreshResult.INVALID_TOKEN, "No active session or token mismatch")
        }

        // 4. Generate New Tokens
        val newAccess = tokenService.generateAccessToken(user.id, user.username)
        val newRefresh = tokenService.generateRefreshToken(user.id)

        // 5. Update State (Rotation)
        val expiresAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(JWT.decode(newRefresh).expiresAt.time),
            ZoneId.systemDefault()
        )
        val newRefreshHash = TokenHasher.hash(newRefresh)
        repository.updateRefreshToken(incomingHash, newRefreshHash, expiresAt)

        return RefreshResponse(
            result = RefreshResult.SUCCESS,
            message = "Refreshed",
            accessToken = newAccess,
            refreshToken = newRefresh
        )
    }
}
