package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        val tokenManager = get<TokenManager>()
        val platform = get<TargetPlatform>()

        HttpClient {
            install(DefaultRequest) {
                url(platform.baseUrl)
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenManager.getAccessToken()
                        val refreshToken = tokenManager.getRefreshToken()
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }

                    refreshTokens {
                        val refreshToken = tokenManager.getRefreshToken() ?: return@refreshTokens null

                        val response = client.post("${platform.baseUrl}/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(refreshToken))
                        }.body<RefreshResponse>()

                        if (response.result == RefreshResult.SUCCESS) {
                            tokenManager.saveTokens(response.accessToken, response.refreshToken)
                            BearerTokens(response.accessToken, response.refreshToken)
                        } else {
                            tokenManager.clearTokens()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        request.url.pathSegments.none { it == "login" || it == "register" || it == "refresh" || it == "verify" || it == "resend-verification" }
                    }
                }
            }
        }
    }
}
