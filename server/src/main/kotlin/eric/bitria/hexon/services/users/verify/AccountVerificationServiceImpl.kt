package eric.bitria.hexon.services.users.verify

import com.auth0.jwt.JWT
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.services.users.profile.ProfileRepository
import eric.bitria.hexon.utils.TokenHasher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AccountVerificationServiceImpl(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService,
    private val tokenService: TokenService,
    private val profileRepository: ProfileRepository
) : AccountVerificationService {

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {

        // 1. Check if User Exists
        val user = authRepository.findUserByEmail(request.email) ?: return VerifyEmailResponse(
            VerifyEmailResult.USER_NOT_FOUND,
            "No account found with this email."
        )

        // 2. Check if Already Verified (Idempotency)
        if (user.isVerified) {
            return VerifyEmailResponse(
                VerifyEmailResult.ALREADY_VERIFIED,
                "Account is already verified."
            )
        }

        // 3. Verify Code via the Service
        val isCodeValid = emailVerificationService.verifyCodeByEmail(
            email = request.email,
            code = request.code,
            type = EmailVerificationType.EMAIL_CONFIRMATION
        )

        if (!isCodeValid) {
            return VerifyEmailResponse(
                VerifyEmailResult.INVALID_CODE,
                "Invalid or expired verification code."
            )
        }

        // 4. Success: Update User State
        authRepository.verifyUser(user.id)

        // 5. Generate Tokens
        val accessToken = tokenService.generateAccessToken(user.id, user.username)
        val refreshToken = tokenService.generateRefreshToken(user.id)

        // 6. Securely Store Refresh Token Session
        val expiresAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(JWT.decode(refreshToken).expiresAt.time),
            ZoneId.systemDefault()
        )
        
        // Periodic cleanup
        authRepository.clearExpiredSessions()

        val refreshTokenHash = TokenHasher.hash(refreshToken)
        authRepository.addRefreshToken(user.id, refreshTokenHash, expiresAt)

        // 7. Create user profile
        profileRepository.createProfile(user.id)

        return VerifyEmailResponse(
            VerifyEmailResult.SUCCESS,
            "Email verified successfully. You can now log in.",
             accessToken,
             refreshToken
        )
    }

    override suspend fun resendVerificationCode(
        request: ResendVerificationCodeRequest
    ): ResendVerificationCodeResponse {

        val user = authRepository.findUserByEmail(request.email)
            ?:
            return ResendVerificationCodeResponse(
                ResendVerificationCodeResult.SUCCESS,
                "A new verification code has been sent to ${request.email}"
            )

        if (user.isVerified) {
            return ResendVerificationCodeResponse(
                ResendVerificationCodeResult.ALREADY_VERIFIED,
                "This account is already verified. You can log in."
            )
        }

        emailVerificationService.sendVerificationCodeByEmail(
            email = request.email,
            type = EmailVerificationType.EMAIL_CONFIRMATION
        )

        return ResendVerificationCodeResponse(
            ResendVerificationCodeResult.SUCCESS,
            "A new verification code has been sent to ${request.email}"
        )
    }
}
