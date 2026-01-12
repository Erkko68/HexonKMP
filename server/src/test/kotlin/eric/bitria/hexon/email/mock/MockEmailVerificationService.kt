package eric.bitria.hexon.email.mock

import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.services.email.verification.EmailVerificationService

class MockEmailVerificationService : EmailVerificationService {
    private val sentCodes = mutableListOf<SentCode>()

    data class SentCode(val email: String, val type: EmailVerificationType, val userId: String? = null)

    override suspend fun sendVerificationCodeByEmail(email: String, type: EmailVerificationType) {
        sentCodes.add(SentCode(email, type))
    }

    override suspend fun sendVerificationCodeByUserId(userId: String, type: EmailVerificationType) {
        sentCodes.add(SentCode("", type, userId))
    }

    override suspend fun verifyCodeByEmail(email: String, code: String, type: EmailVerificationType): Boolean {
        return code == "123456"
    }

    override suspend fun verifyCodeByUserId(userId: String, code: String, type: EmailVerificationType): Boolean {
        return code == "123456"
    }

    fun getSentCodes() = sentCodes
}
