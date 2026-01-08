package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.auth.*

interface AccountRepository {
    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
}
