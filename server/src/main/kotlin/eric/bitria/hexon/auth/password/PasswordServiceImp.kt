package eric.bitria.hexon.auth.password

import eric.bitria.hexon.auth.email.EmailService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.ChangePasswordRequest
import eric.bitria.hexon.dtos.auth.ChangePasswordResponse
import eric.bitria.hexon.dtos.auth.ForgotPasswordRequest
import eric.bitria.hexon.dtos.auth.ForgotPasswordResponse

class PasswordServiceImp(
    emailService: EmailService,
    tokenService: TokenService
) : PasswordService {

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        TODO("Not yet implemented")
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        TODO("Not yet implemented")
    }

}