package eric.bitria.hexon.auth.mock

import com.auth0.jwt.JWT
import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.utils.TokenHasher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MockLoginService(
    private val repository: AuthRepository,
    private val tokenService: TokenService
) : LoginService {
    override suspend fun login(request: LoginRequest): LoginResponse {
        val user = repository.findUserByEmail(request.email)
            ?: return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid email or password")

        // Using simple equality for mock logic, real implementation uses BCrypt
        if (user.password != request.password) {
            return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid email or password")
        }

        if (!user.isVerified) {
            return LoginResponse(LoginResult.NOT_VERIFIED, "Account not verified")
        }

        val accessToken = tokenService.generateAccessToken(user.id, user.username)
        val refreshToken = tokenService.generateRefreshToken(user.id)

        val expiresAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(JWT.decode(refreshToken).expiresAt.time),
            ZoneId.systemDefault()
        )
        val refreshTokenHash = TokenHasher.hash(refreshToken)
        repository.addRefreshToken(user.id, refreshTokenHash, expiresAt)

        return LoginResponse(
            result = LoginResult.SUCCESS,
            message = "Login successful",
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
