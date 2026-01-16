package eric.bitria.hexon.users.mock

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
import eric.bitria.hexon.services.users.verify.AccountVerificationService
import eric.bitria.hexon.utils.TokenHasher

class MockAccountVerificationService(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService,
    private val tokenService: TokenService
) : AccountVerificationService {

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        val user = authRepository.findUserByEmail(request.email)
            ?: return VerifyEmailResponse(VerifyEmailResult.USER_NOT_FOUND, "Not found")

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
        
        val access = tokenService.generateAccessToken(user.id, user.username)
        val refresh = tokenService.generateRefreshToken(user.id)
        authRepository.updateRefreshToken(user.id, TokenHasher.hash(refresh))

        return VerifyEmailResponse(VerifyEmailResult.SUCCESS, "Success", access, refresh)
    }

    override suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        val user = authRepository.findUserByEmail(request.email)
            ?: return ResendVerificationCodeResponse(ResendVerificationCodeResult.SUCCESS, "Sent")

        if (user.isVerified) {
            return ResendVerificationCodeResponse(ResendVerificationCodeResult.ALREADY_VERIFIED, "Already verified")
        }

        emailVerificationService.sendVerificationCodeByEmail(request.email, EmailVerificationType.EMAIL_CONFIRMATION)
        
        return ResendVerificationCodeResponse(ResendVerificationCodeResult.SUCCESS, "Sent")
    }
}
