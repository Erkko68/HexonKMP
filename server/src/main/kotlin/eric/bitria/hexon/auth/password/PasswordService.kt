package eric.bitria.hexon.auth.password

import eric.bitria.hexon.dtos.auth.ChangePasswordRequest
import eric.bitria.hexon.dtos.auth.ChangePasswordResponse
import eric.bitria.hexon.dtos.auth.ForgotPasswordRequest
import eric.bitria.hexon.dtos.auth.ForgotPasswordResponse

interface PasswordService {
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse
}