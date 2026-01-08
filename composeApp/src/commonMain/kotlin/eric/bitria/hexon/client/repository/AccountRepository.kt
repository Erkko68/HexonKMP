package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse

interface AccountRepository {
    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
}
