package eric.bitria.hexon.repository

import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.utils.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorAuthRepository(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(request: LoginRequest): LoginResponse {
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<LoginResponse>()
        
        if (response.result == LoginResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
        }
        
        return response
    }

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<RefreshResponse>()
        
        if (response.result == RefreshResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
        }
        
        return response
    }

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        val response = client.post("/auth/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<VerifyEmailResponse>()

        if (response.result == VerifyEmailResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
        }

        return response
    }

    override suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        return client.post("/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        return client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        return client.post("/auth/change-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
