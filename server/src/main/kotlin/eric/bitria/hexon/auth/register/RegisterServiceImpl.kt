package eric.bitria.hexon.auth.register

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.database.tables.EmailVerificationType
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.email.verification.EmailVerificationService
import eric.bitria.hexon.utils.Validators

class RegisterServiceImpl(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService
) : RegisterService {

    override suspend fun register(request: RegisterRequest): RegisterResponse {

        // 1. Validate Input
        if (!Validators.isValidUsername(request.username)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_USERNAME,
                message = "Invalid username format"
            )
        }

        if (!Validators.isValidEmail(request.email)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_EMAIL,
                message = "Invalid email format"
            )
        }

        if (!Validators.isValidPassword(request.password)) {
            return RegisterResponse(
                result = RegisterResult.INVALID_PASSWORD,
                message = "Password does not meet requirements"
            )
        }

        // 2. Check for Duplicates (Fail Fast)
        if (authRepository.isEmailRegistered(request.email)) {
            return RegisterResponse(
                result = RegisterResult.EMAIL_ALREADY_EXISTS,
                message = "Email is already registered"
            )
        }

        if (authRepository.isUsernameTaken(request.username)) {
            return RegisterResponse(
                result = RegisterResult.USERNAME_ALREADY_EXISTS,
                message = "Username is already taken"
            )
        }

        // 3. Hash Password
        val passwordHash = BCrypt.withDefaults()
            .hashToString(12, request.password.toCharArray())

        // 4. Create User
        authRepository.createUser(
            email = request.email,
            username = request.username,
            passwordHash = passwordHash
        )

        // 5. Trigger Verification Flow
        // This handles generating the code, saving it, and sending the email.
        emailVerificationService.sendVerificationCodeByEmail(
            email = request.email,
            type = EmailVerificationType.EMAIL_CONFIRMATION
        )

        // 6. Return Success
        return RegisterResponse(
            result = RegisterResult.SUCCESS,
            message = "Verification code sent to ${request.email}"
        )
    }
}