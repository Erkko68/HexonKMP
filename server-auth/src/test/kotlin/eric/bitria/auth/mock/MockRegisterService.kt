package eric.bitria.auth.mock

import eric.bitria.auth.email.EmailService
import eric.bitria.auth.register.RegisterRepository
import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse

class MockRegisterService(
    private val repository: RegisterRepository,
    private val tokenService: TokenService,
    private val emailService: EmailService
) : RegisterService {
    override fun register(request: RegisterRequest): RegisterResponse {
        TODO("Not yet implemented")
    }

    override fun verifyEmail(
        email: String,
        code: String
    ): VerifyEmailResponse {
        TODO("Not yet implemented")
    }

    override fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        TODO("Not yet implemented")
    }
}