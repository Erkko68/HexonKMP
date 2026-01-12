package eric.bitria.hexon.services.auth.login

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.utils.TokenHasher
import eric.bitria.hexon.utils.Validators

class LoginServiceImpl(
    private val authRepository: AuthRepository,
    private val tokenService: TokenService
) : LoginService {

    override suspend fun login(request: LoginRequest): LoginResponse {

        // 0. Validate Inputs
        if (!Validators.isValidEmail(request.email)) return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid email format.")
        if (!Validators.isValidPassword(request.password)) return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid password format.")

        // 1. Find User
        val user = authRepository.findUserByEmail(request.email)
            ?: return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid email or password")

        // 2. Verify Password (BCrypt)
        val passwordVerified = BCrypt.verifyer().verify(
            request.password.toCharArray(),
            user.password
        ).verified

        if (!passwordVerified) {
            return LoginResponse(LoginResult.INVALID_CREDENTIALS, "Invalid email or password")
        }

        // 3. Check Account Status
        if (!user.isVerified) {
            return LoginResponse(
                LoginResult.NOT_VERIFIED,
                "Account exists but is not verified. Please check your email."
            )
        }

        // 4. Generate Tokens
        val accessToken = tokenService.generateAccessToken(user.id)
        val refreshToken = tokenService.generateRefreshToken(user.id)

        // 5. Securely Store Refresh Token Session
        val refreshTokenHash = TokenHasher.hash(refreshToken)

        authRepository.updateRefreshToken(user.id, refreshTokenHash)

        // 6. Return Success
        return LoginResponse(
            result = LoginResult.SUCCESS,
            message = "Login successful",
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}