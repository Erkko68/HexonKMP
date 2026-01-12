package eric.bitria.hexon.email.mock

import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.services.email.verification.EmailVerificationService

class MockEmailVerificationService : EmailVerificationService {
    private val sentCodes = mutableListOf<SentCode>()
    private val smtpService = MockSmtpService()

    data class SentCode(val email: String, val type: EmailVerificationType, val userId: String? = null, val code: String)

    override suspend fun sendVerificationCodeByEmail(email: String, type: EmailVerificationType) {
        val code = "123456"
        sentCodes.add(SentCode(email, type, code = code))
        smtpService.sendEmail(email, "Verification Code", "Your verification code is: $code")
    }

    override suspend fun sendVerificationCodeByUserId(userId: String, type: EmailVerificationType) {
        val code = "123456"
        sentCodes.add(SentCode("", type, userId, code = code))
        // Note: in a real mock we'd need the email here, but for simple tests we just track it.
    }

    override suspend fun verifyCodeByEmail(email: String, code: String, type: EmailVerificationType): Boolean {
        return code == "123456"
    }

    override suspend fun verifyCodeByUserId(userId: String, code: String, type: EmailVerificationType): Boolean {
        return code == "123456"
    }

    fun getSentCodes() = sentCodes
    fun getSmtpService() = smtpService
}
