package eric.bitria.auth.mock

import eric.bitria.auth.database.AuthRepository
import eric.bitria.auth.email.EmailService
import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.token.TokenService
import eric.bitria.hexon.utils.Validators.isValidCode
import eric.bitria.hexon.utils.Validators.isValidEmail
import eric.bitria.hexon.utils.Validators.isValidPassword
import eric.bitria.hexon.utils.Validators.isValidUsername
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult

class MockRegisterService(
    private val repository: AuthRepository,
    private val tokenService: TokenService,
    private val emailService: EmailService
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

        // Check for duplicates
        if (repository.emailExists(request.email)) {
            return RegisterResponse(
                result = RegisterResult.EMAIL_EXISTS,
                message = "Email is already registered: ${request.email}",
            )
        }

        if (repository.usernameExists(request.username)) {
            return RegisterResponse(
                result = RegisterResult.USERNAME_EXISTS,
                message = "Username is already taken: ${request.username}",
            )
        }

        // Password validation
        if (!isValidPassword(request.password)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_PASSWORD,
                message = "Invalid password",
            )
        }

        // Generate verification code
        val verificationCode = generateVerificationCode()

        // Save user with code
        repository.saveUser(request.email, request.username, request.password, verificationCode)

        // Send email to user
        emailService.sendEmail(
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

        return when (repository.verifyEmail(email, code)) {
            VerifyEmailResult.INVALID_EMAIL ->
                VerifyEmailResponse(
                    result = VerifyEmailResult.INVALID_EMAIL,
                    message = "Invalid or unknown email",
                    accessToken = "",
                    refreshToken = ""
                )
            VerifyEmailResult.INVALID_VERIFICATION_CODE ->
                VerifyEmailResponse(
                    result = VerifyEmailResult.INVALID_VERIFICATION_CODE,
                    message = email,
                    accessToken = "",
                    refreshToken = ""
                )
            VerifyEmailResult.SUCCESS ->
                VerifyEmailResponse(
                    result = VerifyEmailResult.SUCCESS,
                    message = email,
                    accessToken = tokenService.generateAccessToken(
                        userId = email
                    ),
                    refreshToken = tokenService.generateRefreshToken(
                        userId = email
                    )
                )
            VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED ->
                VerifyEmailResponse(
                    result = VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                    message = email,
                    accessToken = "",
                    refreshToken = ""
                )
            else -> {
                VerifyEmailResponse(
                    result = VerifyEmailResult.UNKNOWN_ERROR,
                    message = "Unexpected Response",
                    accessToken = "",
                    refreshToken = ""
                )
            }
        }
    }

    override suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        if (!isValidEmail(request.email)) {
            return ResendVerificationCodeResponse(
                result = ResendVerificationCodeResult.INVALID_EMAIL,
                message = "Invalid email",
            )
        }

        if (!repository.emailExists(request.email)) {
            return ResendVerificationCodeResponse(
                result = ResendVerificationCodeResult.EMAIL_NOT_REGISTERED,
                message = "Email is not registered: ${request.email}",
            )
        }

        if (repository.isAccountVerified(request.email)) {
            return ResendVerificationCodeResponse(
                result = ResendVerificationCodeResult.EMAIL_ALREADY_VERIFIED,
                message = "Email already verified.",
            )
        }

        val verificationCode = generateVerificationCode()

        // Regenerate verification code
        repository.updateVerificationCode(request.email,verificationCode)

        emailService.sendEmail(
            to = request.email,
            subject = "Email Verification",
            body = verificationCode
        )

        return ResendVerificationCodeResponse(
            result = ResendVerificationCodeResult.SUCCESS,
            message = "Resent verification code.",
        )
    }

    private fun generateVerificationCode() = (100000..999999).random().toString()
}