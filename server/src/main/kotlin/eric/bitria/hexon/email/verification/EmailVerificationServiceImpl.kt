package eric.bitria.hexon.email.verification

import eric.bitria.hexon.database.tables.EmailVerificationType
import eric.bitria.hexon.email.repository.EmailVerificationRepository
import eric.bitria.hexon.email.smtp.SmtpService
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class EmailVerificationServiceImpl(
    private val verificationRepo: EmailVerificationRepository,
    private val smtpService: SmtpService,
    private val usersRepository: Any // Replace with actual UserRepository
) : EmailVerificationService {

    private val codeValidityDuration = 15.minutes

    // --- SENDING ---

    override suspend fun sendVerificationCodeByUserId(userId: String, type: EmailVerificationType) {
        // val email = userRepository.getEmailById(userId)
        //     ?: throw IllegalArgumentException("User not found")
        val email = "mock_$userId@example.com" // Placeholder

        // Reuse the logic
        sendVerificationCodeByEmail(email, type)
    }

    override suspend fun sendVerificationCodeByEmail(email: String, type: EmailVerificationType) {
        val rawCode = Random.nextInt(100000, 999999).toString()
        val codeHash = rawCode.reversed() // Replace with real hashing (BCrypt/SHA)

        // Calculate expiry
        // Note: Use standard java.time or kotlin.time.Instant based on your preference
        val expiresAt = kotlin.time.Clock.System.now() + codeValidityDuration

        verificationRepo.saveVerificationCode(email, codeHash, type, expiresAt)

        smtpService.sendEmail(
            email,
            "Your Verification Code",
            "Your code is: $rawCode"
        )
    }

    // --- VERIFYING ---

    override suspend fun verifyCodeByUserId(userId: String, code: String, type: EmailVerificationType): Boolean {
        // val email = userRepository.getEmailById(userId) ?: return false
        val email = "mock_$userId@example.com" // Placeholder

        return verifyCodeByEmail(email, code, type)
    }

    override suspend fun verifyCodeByEmail(email: String, code: String, type: EmailVerificationType): Boolean {
        val storedHash = verificationRepo.getVerificationCodeHash(email, type) ?: return false

        val inputHash = code.reversed() // Replace with real hashing check

        if (inputHash == storedHash) {
            verificationRepo.deleteVerificationCode(email)
            return true
        } else {
            verificationRepo.incrementAttempts(email)
            return false
        }
    }
}