package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorAccountRepository(
    private val client: HttpClient
) : AccountRepository {

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        return client.post("/account/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        return client.post("/account/change-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
