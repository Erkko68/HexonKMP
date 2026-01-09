package eric.bitria.hexon.email.verification

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.database.tables.EmailVerificationType
import eric.bitria.hexon.email.repository.EmailVerificationRepository
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.users.repository.UserRepository
import kotlin.random.Random
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.minutes

class EmailVerificationServiceImpl(
    private val verificationRepo: EmailVerificationRepository,
    private val smtpService: SmtpService,
    private val userRepository: UserRepository
) : EmailVerificationService {

    private val codeValidityDuration = 15.minutes
    private val bcryptCost = 10

    // --- SENDING ---

    override suspend fun sendVerificationCodeByUserId(userId: String, type: EmailVerificationType) {
        val email = userRepository.getEmailByUserId(userId)
            ?: throw IllegalArgumentException("User not found with id: $userId")

        sendVerificationCodeByEmail(email, type)
    }

    override suspend fun sendVerificationCodeByEmail(email: String, type: EmailVerificationType) {
        val rawCode = Random.nextInt(100000, 999999).toString()

        val codeHash = BCrypt.withDefaults()
            .hashToString(bcryptCost, rawCode.toCharArray())

        // Calculate expiry
        val expiresAt = now() + codeValidityDuration

        verificationRepo.saveVerificationCode(email, codeHash, type, expiresAt)

        smtpService.sendEmail(
            email,
            getSubjectForType(type),
            "Your verification code is: $rawCode"
        )
    }

    // --- VERIFYING ---

    override suspend fun verifyCodeByUserId(userId: String, code: String, type: EmailVerificationType): Boolean {
        val email = userRepository.getEmailByUserId(userId)
            ?: throw IllegalArgumentException("User not found with id: $userId")

        return verifyCodeByEmail(email, code, type)
    }

    override suspend fun verifyCodeByEmail(
        email: String,
        code: String,
        type: EmailVerificationType
    ): Boolean {
        // --- CHECK 1: EXISTENCE ---
        val storedCode = verificationRepo.getVerificationCode(email) ?: return false

        // --- CHECK 2: TYPE MATCH ---
        if (storedCode.type != type) return false

        // --- CHECK 3: EXPIRY ---
        val now = now()
        if (now > storedCode.expiresAt) {
            verificationRepo.deleteVerificationCode(email)
            return false
        }

        // --- CHECK 4: HASH VALIDATION ---
        val result = BCrypt.verifyer().verify(code.toCharArray(), storedCode.codeHash)

        if (result.verified) {
            // SUCCESS: Consume the code
            verificationRepo.deleteVerificationCode(email)
            return true
        } else {
            // FAILURE: Wrong code
            verificationRepo.incrementAttempts(email)
            return false
        }
    }

    // --- HELPERS ---

    private fun getSubjectForType(type: EmailVerificationType): String {
        return when (type) {
            EmailVerificationType.EMAIL_CONFIRMATION -> "Confirm your email"
            EmailVerificationType.PASSWORD_RESET -> "Reset your password"
            EmailVerificationType.ACCOUNT_DELETION -> "Confirm account deletion"
        }
    }
}