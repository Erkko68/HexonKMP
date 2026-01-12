package eric.bitria.hexon.email.mock

import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.services.email.repository.EmailVerificationRepository
import eric.bitria.hexon.services.email.repository.StoredVerificationCode
import kotlin.time.Clock
import kotlin.time.Instant

class MockEmailVerificationRepository : EmailVerificationRepository {
    private val codes = mutableMapOf<String, StoredVerificationCode>()
    private val attempts = mutableMapOf<String, Int>()

    override suspend fun saveVerificationCode(
        email: String,
        codeHash: String,
        type: EmailVerificationType,
        expiresAt: Instant
    ) {
        codes[email] = StoredVerificationCode(codeHash, type, expiresAt)
        attempts[email] = 0
    }

    override suspend fun getVerificationCode(email: String): StoredVerificationCode? {
        return codes[email]
    }

    override suspend fun incrementAttempts(email: String) {
        attempts[email] = (attempts[email] ?: 0) + 1
    }

    override suspend fun deleteVerificationCode(email: String) {
        codes.remove(email)
        attempts.remove(email)
    }

    override suspend fun deleteExpiredCodes() {
        val now = Clock.System.now()
        codes.entries.removeIf { it.value.expiresAt < now }
    }

    fun getAttempts(email: String): Int = attempts[email] ?: 0
}
