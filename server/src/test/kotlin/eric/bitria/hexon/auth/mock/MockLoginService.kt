package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult

class MockLoginService(private val repository: AuthRepository) : LoginService {
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

        return LoginResponse(
            result = LoginResult.SUCCESS,
            message = "Login successful",
            accessToken = "mock-access-token-${user.id}",
            refreshToken = "mock-refresh-token-${user.id}"
        )
    }
}
