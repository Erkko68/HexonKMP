package eric.bitria.hexon.repository

import eric.bitria.hexon.dtos.auth.*

interface AuthRepository {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun refresh(request: RefreshRequest): RefreshResponse
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse
    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse
    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
}