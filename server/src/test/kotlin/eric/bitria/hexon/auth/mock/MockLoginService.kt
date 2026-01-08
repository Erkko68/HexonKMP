package eric.bitria.hexon.auth.mock

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.email.EmailService
import eric.bitria.hexon.auth.login.LoginService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult

class MockLoginService(
    private val repository: AuthRepository,
    private val tokenService: TokenService,
    private val emailService: EmailService
) : LoginService {

    override suspend fun login(request: LoginRequest): LoginResponse {

        val hashedPassword = repository.getPasswordByEmail(request.email)
            ?: return LoginResponse(
                result = LoginResult.INVALID_EMAIL_OR_PASSWORD,
                message = "Invalid email or password",
                accessToken = "",
                refreshToken = ""
            )

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), hashedPassword)
        if (!result.verified) {
            return LoginResponse(
                result = LoginResult.INVALID_EMAIL_OR_PASSWORD,
                message = "Invalid email or password",
                accessToken = "",
                refreshToken = ""
            )
        }

        if (!repository.isAccountVerified(request.email)) {
            val verificationCode = (100000..999999).random().toString()
            repository.updateUserCodeByEmail(request.email, verificationCode)
            emailService.sendEmail(
                to = request.email,
                subject = "Email Verification",
                body = verificationCode
            )
            return LoginResponse(
                result = LoginResult.PENDING_VERIFICATION,
                message = "Account not verified. A new verification code has been sent.",
                accessToken = "",
                refreshToken = ""
            )
        }

        val userId = repository.getUserIdByEmail(request.email)

        return LoginResponse(
            result = LoginResult.SUCCESS,
            message = "Login successful",
            accessToken = tokenService.generateAccessToken(userId),
            refreshToken = tokenService.generateRefreshToken(userId)
        )
    }
}
