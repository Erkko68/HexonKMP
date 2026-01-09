package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.auth.*

interface AuthRepository {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun refresh(request: RefreshRequest): RefreshResponse
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
    suspend fun resendVerificationCode(request: SendEmailVerificationCodeRequest): SendEmailVerificationCodeResponse
    suspend fun autoLogin(): Boolean
}