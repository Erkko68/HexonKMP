package eric.bitria.hexon.client

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorUserClient(
    private val client: HttpClient
) : UserClient {

    override suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        return client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
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

    /*
    suspend fun forgotPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        return client.post("/users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    */

}