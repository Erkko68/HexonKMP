package eric.bitria.hexon.auth.login

import eric.bitria.hexon.auth.password.PasswordService
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.utils.Validators.isValidEmail
import eric.bitria.hexon.utils.Validators.isValidPassword
import eric.bitria.hexon.utils.hashToken

class LoginServiceImp(
    private val authRepository: AuthRepository,
    private val passwordService: PasswordService,
    private val tokenService: TokenService,
    private val smtpService: SmtpService
) : LoginService {
    override suspend fun login(request: LoginRequest): LoginResponse {
        
        if (!isValidEmail(request.email)) {
            return LoginResponse(
                result = LoginResult.INVALID_EMAIL_OR_PASSWORD,
                message = "Invalid email format",
                accessToken = "",
                refreshToken = ""
            )
        }

        if (!isValidPassword(request.password)) {
            return LoginResponse(
                result = LoginResult.INVALID_EMAIL_OR_PASSWORD,
                message = "Invalid password format",
                accessToken = "",
                refreshToken = ""
            )
        }

        val userId = authRepository.getUserIdByEmail(request.email)

        if (userId == null || (!passwordService.verifyPassword(userId, request.password))) {
            return LoginResponse(
                result = LoginResult.INVALID_EMAIL_OR_PASSWORD,
                message = "Invalid email or password",
                accessToken = "",
                refreshToken = ""
            )
        }

        if (!authRepository.isAccountVerified(request.email)) {
            val verificationCode = (100000..999999).random().toString()
            authRepository.updateUserCodeByEmail(request.email, verificationCode)
            smtpService.sendEmail(
                to = request.email,
                subject = "Email Verification",
                body = "Your verification code is: $verificationCode"
            )
            return LoginResponse(
                result = LoginResult.PENDING_VERIFICATION,
                message = "Account not verified. A new verification code has been sent.",
                accessToken = "",
                refreshToken = ""
            )
        }

        val accessToken = tokenService.generateAccessToken(userId)
        val refreshToken = tokenService.generateRefreshToken(userId)

        val refreshTokenHash = hashToken(refreshToken)
        authRepository.updateRefreshTokenHash(userId, refreshTokenHash)
        
        return LoginResponse(
            result = LoginResult.SUCCESS,
            message = "Login successful",
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}