package eric.bitria.hexon.users.verify

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.email.verification.EmailVerificationService

class AccountVerificationServiceImpl(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService,
    private val tokenService: TokenService
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
        // This handles Hash Checking, Expiry, and Type Matching
        val isCodeValid = emailVerificationService.verifyCodeByEmail(
            email = request.email,
            code = request.code,
            type = EmailVerificationType.EMAIL_CONFIRMATION
        )

        if (!isCodeValid) {
            // Note: Our service returns false for both "Wrong Code" and "Expired".
            // If you need to distinguish, verifyCodeByEmail would need to return a Result object.
            return VerifyEmailResponse(
                VerifyEmailResult.INVALID_CODE,
                "Invalid or expired verification code."
            )
        }

        // 4. Success: Update User State
        authRepository.verifyUser(user.id)

        // 5. Generate Tokens
        val accessToken = tokenService.generateAccessToken(user.id)
        val refreshToken = tokenService.generateRefreshToken(user.id)

        // 6. Securely Store Refresh Token Session
        val refreshTokenHash = BCrypt.withDefaults()
            .hashToString(10, refreshToken.toCharArray())

        authRepository.updateRefreshToken(user.id, refreshTokenHash)

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

        // 1. Check User Existence
        val user = authRepository.findUserByEmail(request.email)
            ?:
            return ResendVerificationCodeResponse(
                ResendVerificationCodeResult.SUCCESS,
                "A new verification code has been sent to ${request.email}"
            )

        // 2. Check if Already Verified
        if (user.isVerified) {
            return ResendVerificationCodeResponse(
                ResendVerificationCodeResult.ALREADY_VERIFIED,
                "This account is already verified. You can log in."
            )
        }

        // 3. Generate & Send New Code
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