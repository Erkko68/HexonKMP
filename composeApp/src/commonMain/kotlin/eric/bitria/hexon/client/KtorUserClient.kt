package eric.bitria.hexon.client

import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.RequestDeleteAccountResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import eric.bitria.hexon.dtos.profile.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class KtorUserClient(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) : UserClient {

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        val response = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<VerifyEmailResponse>()

        if (response.result == VerifyEmailResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken!!, response.refreshToken!!)
        }

        return response
    }

    override suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        return client.post("/users/email/resend") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        return client.post("/users/password/change") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        return client.post("/users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        return client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun initiateDeleteAccount(): RequestDeleteAccountResponse {
        return client.post("/users/me/delete/initiate").body()
    }

    override suspend fun confirmDeleteAccount(request: ConfirmDeleteAccountRequest): ConfirmDeleteAccountResponse {
        return client.delete("/users/me") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getMe(): UserProfileResponse {
        return client.get("/users/me").body()
    }

    override suspend fun getPublicProfile(userId: String): PublicUserProfileResponse? {
        return try {
            client.get("/users/$userId").body()
        } catch (e: ClientRequestException) {
            null
        }
    }
}