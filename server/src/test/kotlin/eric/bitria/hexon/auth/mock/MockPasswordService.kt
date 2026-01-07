package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.email.EmailService
import eric.bitria.hexon.auth.password.PasswordService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.ChangePasswordRequest
import eric.bitria.hexon.dtos.auth.ChangePasswordResponse
import eric.bitria.hexon.dtos.auth.ForgotPasswordRequest
import eric.bitria.hexon.dtos.auth.ForgotPasswordResponse

class MockPasswordService(
    tokenService: TokenService,
    emailService: EmailService
) : PasswordService {

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        TODO("Not yet implemented")
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        TODO("Not yet implemented")
    }
}