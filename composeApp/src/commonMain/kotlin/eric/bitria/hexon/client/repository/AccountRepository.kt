package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse

interface AccountRepository {
    suspend fun forgotPassword(request: ResetPasswordRequest): ResetPasswordResponse
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
}
