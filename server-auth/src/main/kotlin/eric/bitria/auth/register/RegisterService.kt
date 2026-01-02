package eric.bitria.auth.register

import eric.bitria.hexon.dtos.auth.*

interface RegisterService {

    /**
     * Registers a new user with the provided request.
     */
    fun register(request: RegisterRequest): RegisterResponse

    /**
     * Verifies a user's email using the given code.
     */
    fun verifyEmail(email: String, code: String): VerifyEmailResponse

    /**
     * Resends the verification code to the user's email.
     */
    fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse
}
