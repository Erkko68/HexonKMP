package eric.bitria.hexon.users.password

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse

interface PasswordService {
    suspend fun changePassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse
}