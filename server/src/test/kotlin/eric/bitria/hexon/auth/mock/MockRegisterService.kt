package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.register.RegisterService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.email.verification.EmailVerificationService
import eric.bitria.hexon.utils.Validators

class MockRegisterService(
    private val repository: AuthRepository,
    private val emailService: EmailVerificationService? = null
) : RegisterService {
    override suspend fun register(request: RegisterRequest): RegisterResponse {
        if (!Validators.isValidEmail(request.email)) {
            return RegisterResponse(RegisterResult.INVALID_EMAIL, "Invalid email format")
        }
        if (!Validators.isValidUsername(request.username)) {
            return RegisterResponse(RegisterResult.INVALID_USERNAME, "Invalid username format")
        }
        if (!Validators.isValidPassword(request.password)) {
            return RegisterResponse(RegisterResult.INVALID_PASSWORD, "Password does not meet requirements")
        }
        
        if (repository.isEmailRegistered(request.email)) {
            return RegisterResponse(RegisterResult.EMAIL_ALREADY_EXISTS, "Email is already registered")
        }
        if (repository.isUsernameTaken(request.username)) {
            return RegisterResponse(RegisterResult.USERNAME_ALREADY_EXISTS, "Username is already taken")
        }

        repository.createUser(request.email, request.username, request.password)
        
        emailService?.sendVerificationCodeByEmail(
            email = request.email,
            type = EmailVerificationType.EMAIL_CONFIRMATION
        )
        
        return RegisterResponse(RegisterResult.SUCCESS, "Verification code sent")
    }
}
