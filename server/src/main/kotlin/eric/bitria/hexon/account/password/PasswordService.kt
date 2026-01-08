package eric.bitria.hexon.account.password

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse

interface PasswordService {
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
    suspend fun forgotPasswordCodeRequest(request: ForgotPasswordRequest): ForgotPasswordResponse
}