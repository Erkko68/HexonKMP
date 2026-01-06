package eric.bitria.hexon.auth.register

import eric.bitria.hexon.dtos.auth.*

interface RegisterService {

    /**
     * Registers a new user with the provided request.
     */
    suspend fun register(request: RegisterRequest): RegisterResponse

    /**
     * Verifies a user's email using the given code.
     */
    suspend fun verifyEmail(email: String, code: String): VerifyEmailResponse

    /**
     * Resends the verification code to the user's email.
     */
    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse
}
