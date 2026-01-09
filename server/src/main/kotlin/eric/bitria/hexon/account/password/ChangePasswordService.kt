package eric.bitria.hexon.account.password

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse

interface ChangePasswordService {
    suspend fun changeWithOldPassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse
    suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse
}