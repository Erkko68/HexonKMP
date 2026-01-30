package eric.bitria.hexon.users.mock

import com.auth0.jwt.JWT
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.services.users.verify.AccountVerificationService
import eric.bitria.hexon.utils.TokenHasher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MockAccountVerificationService(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService,
    private val tokenService: TokenService
) : AccountVerificationService {

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        val user = authRepository.findUserByEmail(request.email)
            ?: return VerifyEmailResponse(VerifyEmailResult.USER_NOT_FOUND, "User not found")

        if (user.isVerified) {
            return VerifyEmailResponse(VerifyEmailResult.ALREADY_VERIFIED, "Already verified")
        }

        val isValid = emailVerificationService.verifyCodeByEmail(
            request.email,
            request.code,
            EmailVerificationType.EMAIL_CONFIRMATION
        )

        if (!isValid) {
            return VerifyEmailResponse(VerifyEmailResult.INVALID_CODE, "Invalid code")
        }

        authRepository.verifyUser(user.id)

        val accessToken = tokenService.generateAccessToken(user.id, user.username)
        val refreshToken = tokenService.generateRefreshToken(user.id)

        val expiresAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(JWT.decode(refreshToken).expiresAt.time),
            ZoneId.systemDefault()
        )
        val refreshTokenHash = TokenHasher.hash(refreshToken)
        authRepository.addRefreshToken(user.id, refreshTokenHash, expiresAt)

        return VerifyEmailResponse(
            VerifyEmailResult.SUCCESS,
            "Verified",
            accessToken,
            refreshToken
        )
    }

    override suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        val user = authRepository.findUserByEmail(request.email)
            ?: return ResendVerificationCodeResponse(ResendVerificationCodeResult.SUCCESS, "Sent (simulated)")

        if (user.isVerified) {
            return ResendVerificationCodeResponse(ResendVerificationCodeResult.ALREADY_VERIFIED, "Already verified")
        }

        emailVerificationService.sendVerificationCodeByEmail(request.email, EmailVerificationType.EMAIL_CONFIRMATION)
        return ResendVerificationCodeResponse(ResendVerificationCodeResult.SUCCESS, "Sent")
    }
}
