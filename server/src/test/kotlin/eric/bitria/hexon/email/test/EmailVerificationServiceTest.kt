package eric.bitria.hexon.email.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.services.email.verification.EmailVerificationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class EmailVerificationServiceTest {

    private lateinit var verificationRepo: MockEmailVerificationRepository
    private lateinit var smtpService: MockSmtpService
    private lateinit var authRepository: AuthRepository
    private lateinit var service: EmailVerificationServiceImpl

    private val testEmail = "test@example.com"
    private lateinit var testUserId: String

    @BeforeEach
    fun setup() = runBlocking {
        verificationRepo = MockEmailVerificationRepository()
        smtpService = MockSmtpService()
        authRepository = MockAuthRepository()
        service = EmailVerificationServiceImpl(verificationRepo, smtpService, authRepository)
        val user = authRepository.createUser(testEmail, "testuser", "password")
        testUserId = user.id
    }

    @Test
    fun `sendVerificationCodeByEmail should save and send code`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.EMAIL_CONFIRMATION)

        val storedCode = verificationRepo.getVerificationCode(testEmail)
        assertNotNull(storedCode)
        assertEquals(EmailVerificationType.EMAIL_CONFIRMATION, storedCode?.type)

        val sentEmail = smtpService.getLastEmailTo(testEmail)
        assertNotNull(sentEmail)
        assertTrue(sentEmail!!.body.contains("Your verification code is:"))

        // Extract code from email body
        val code = sentEmail.body.substringAfter(": ").trim()
        assertEquals(6, code.length)
    }

    @Test
    fun `sendVerificationCodeByUserId should resolve email and send`() = runBlocking {
        service.sendVerificationCodeByUserId(testUserId, EmailVerificationType.PASSWORD_RESET)

        val sentEmail = smtpService.getLastEmailTo(testEmail)
        assertNotNull(sentEmail)
        assertEquals("Reset your password", sentEmail!!.subject)
    }

    @Test
    fun `sendVerificationCodeByUserId should throw if user not found`() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.sendVerificationCodeByUserId("unknown", EmailVerificationType.EMAIL_CONFIRMATION)
            }
        }
        Unit
    }

    @Test
    fun `verifyCodeByEmail should return true and delete code on success`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.EMAIL_CONFIRMATION)
        val sentEmail = smtpService.getLastEmailTo(testEmail)
        val code = sentEmail!!.body.substringAfter(": ").trim()

        val result = service.verifyCodeByEmail(testEmail, code, EmailVerificationType.EMAIL_CONFIRMATION)

        assertTrue(result)
        assertNull(verificationRepo.getVerificationCode(testEmail))
    }

    @Test
    fun `verifyCodeByEmail should return false if code is wrong and increment attempts`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.EMAIL_CONFIRMATION)

        val result = service.verifyCodeByEmail(testEmail, "000000", EmailVerificationType.EMAIL_CONFIRMATION)

        assertFalse(result)
        assertNotNull(verificationRepo.getVerificationCode(testEmail))
        assertEquals(1, verificationRepo.getAttempts(testEmail))
    }

    @Test
    fun `verifyCodeByEmail should return false if type mismatch`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.EMAIL_CONFIRMATION)
        val sentEmail = smtpService.getLastEmailTo(testEmail)
        val code = sentEmail!!.body.substringAfter(": ").trim()

        val result = service.verifyCodeByEmail(testEmail, code, EmailVerificationType.PASSWORD_RESET)

        assertFalse(result)
        assertNotNull(verificationRepo.getVerificationCode(testEmail))
    }

    @Test
    fun `verifyCodeByEmail should return false and delete code if expired`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.EMAIL_CONFIRMATION)
        val stored = verificationRepo.getVerificationCode(testEmail)!!
        
        // Force expiry by updating the repo directly
        verificationRepo.saveVerificationCode(
            testEmail, 
            stored.codeHash, 
            stored.type, 
            kotlin.time.Clock.System.now() - 1.minutes
        )

        val result = service.verifyCodeByEmail(testEmail, "any", EmailVerificationType.EMAIL_CONFIRMATION)

        assertFalse(result)
        assertNull(verificationRepo.getVerificationCode(testEmail))
    }

    @Test
    fun `verifyCodeByUserId should work correctly`() = runBlocking {
        service.sendVerificationCodeByEmail(testEmail, EmailVerificationType.ACCOUNT_DELETION)
        val sentEmail = smtpService.getLastEmailTo(testEmail)
        val code = sentEmail!!.body.substringAfter(": ").trim()

        val result = service.verifyCodeByUserId(testUserId, code, EmailVerificationType.ACCOUNT_DELETION)

        assertTrue(result)
    }
}
