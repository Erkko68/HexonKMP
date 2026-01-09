package eric.bitria.hexon.auth.register

import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.database.tables.EmailVerificationType
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.email.verification.EmailVerificationService

class AccountVerificationServiceImpl(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService
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

        return VerifyEmailResponse(
            VerifyEmailResult.SUCCESS,
            "Email verified successfully. You can now log in."
        )
    }
}