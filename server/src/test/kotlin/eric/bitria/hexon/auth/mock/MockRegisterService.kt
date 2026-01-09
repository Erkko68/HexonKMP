package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.auth.register.RegisterService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.utils.Validators.isValidCode
import eric.bitria.hexon.utils.Validators.isValidEmail
import eric.bitria.hexon.utils.Validators.isValidPassword
import eric.bitria.hexon.utils.Validators.isValidUsername
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.SendEmailVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.SendEmailVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.SendEmailVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult

class MockRegisterService(
    private val repository: AuthRepository,
    private val tokenService: TokenService,
    private val smtpService: SmtpService
) : RegisterService {

    /**
     * Registers a new user.
     * Performs validation, checks for duplicates, and stores verification code.
     */
    override suspend fun register(request: RegisterRequest): RegisterResponse {

        // Validate input
        if (!isValidUsername(request.username)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_USERNAME,
                message = "Invalid username",
            )
        }

        if (!isValidEmail(request.email)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_EMAIL,
                message = "Invalid email",
            )
        }

        if(!isValidPassword(request.password)){
            return RegisterResponse(
                result = RegisterResult.INVALID_PASSWORD,
                message = "Invalid password"
            )
        }

        // Check if verified email exists
        if (repository.emailExists(request.email) && repository.isAccountVerified(request.email)) {
            return RegisterResponse(
                result = RegisterResult.EMAIL_EXISTS,
                message = "Email is already registered: ${request.email}",
            )
        }

        val ownerEmail = repository.getEmailByUsername(request.username)
        if (ownerEmail != null && ownerEmail != request.email) {
            return RegisterResponse(
                result = RegisterResult.USERNAME_EXISTS,
                message = "Username is already taken: ${request.username}",
            )
        }

        // Generate verification code
        val verificationCode = generateVerificationCode()

        // Save or update user with code
        repository.saveOrUpdateUnverifiedUser(request.email, request.username, request.password, verificationCode)

        // Send email to user
        smtpService.sendEmail(
            to = request.email,
            subject = "Email Verification",
            body = verificationCode
        )

        return RegisterResponse(
            result = RegisterResult.VERIFICATION_SENT,
            message = "Verification code sent to ${request.email}",
        )
    }

    /**
     * Verifies a user's email with the provided code.
     */
    override suspend fun verifyEmail(email: String, code: String): VerifyEmailResponse {
        // Validate input
        if (!isValidEmail(email)){
            return VerifyEmailResponse(
                result = VerifyEmailResult.INVALID_EMAIL,
                message = "Invalid or unknown email",
                accessToken = "",
                refreshToken = ""
            )
        }

        if(!isValidCode(code)){
            return VerifyEmailResponse(
                result = VerifyEmailResult.INVALID_VERIFICATION_CODE,
                message = "Invalid verification code",
                accessToken = "",
                refreshToken = ""
            )
        }

        if (!repository.emailExists(email)) {
            return VerifyEmailResponse(
                result = VerifyEmailResult.INVALID_EMAIL,
                message = "Invalid or unknown email",
                accessToken = "",
                refreshToken = ""
            )
        }

        if (repository.isAccountVerified(email)) {
            return VerifyEmailResponse(
                result = VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                message = "Account already verified",
                accessToken = "",
                refreshToken = ""
            )
        }

        val savedCode = repository.getVerificationCodeByEmail(email)

        return if (savedCode == code) {
            repository.markAccountAsVerified(email)
            VerifyEmailResponse(
                result = VerifyEmailResult.SUCCESS,
                message = "",
                accessToken = tokenService.generateAccessToken(
                    userId = repository.getUserIdByEmail(email)
                ),
                refreshToken = tokenService.generateRefreshToken(
                    userId = repository.getUserIdByEmail(email)
                )
            )
        } else {
            VerifyEmailResponse(
                result = VerifyEmailResult.INVALID_VERIFICATION_CODE,
                message = "Invalid verification code",
                accessToken = "",
                refreshToken = ""
            )
        }
    }

    override suspend fun resendVerificationCode(request: SendEmailVerificationCodeRequest): SendEmailVerificationCodeResponse {
        if (!isValidEmail(request.email)) {
            return SendEmailVerificationCodeResponse(
                result = SendEmailVerificationCodeResult.INVALID_EMAIL,
                message = "Invalid email",
            )
        }

        if (!repository.emailExists(request.email)) {
            return SendEmailVerificationCodeResponse(
                result = SendEmailVerificationCodeResult.EMAIL_NOT_REGISTERED,
                message = "Email is not registered: ${request.email}",
            )
        }

        if (repository.isAccountVerified(request.email)) {
            return SendEmailVerificationCodeResponse(
                result = SendEmailVerificationCodeResult.EMAIL_ALREADY_VERIFIED,
                message = "Email already verified.",
            )
        }

        val verificationCode = generateVerificationCode()

        // Regenerate verification code
        repository.updateUserCodeByEmail(request.email,verificationCode)

        smtpService.sendEmail(
            to = request.email,
            subject = "Email Verification",
            body = verificationCode
        )

        return SendEmailVerificationCodeResponse(
            result = SendEmailVerificationCodeResult.SUCCESS,
            message = "Resent verification code.",
        )
    }

    private fun generateVerificationCode() = (100000..999999).random().toString()
}
