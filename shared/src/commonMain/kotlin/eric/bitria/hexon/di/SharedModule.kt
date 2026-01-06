package eric.bitria.hexon.di

import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.repository.AuthRepository
import eric.bitria.hexon.repository.KtorAuthRepository
import eric.bitria.hexon.utils.TokenManager
import eric.bitria.hexon.utils.TokenManagerImpl
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val sharedModule = module {
    single<TokenManager> { TokenManagerImpl() }

    single {
        val tokenManager = get<TokenManager>()
        HttpClient {
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

                        val response = client.post("http://10.0.2.2:8080/auth/refresh") {
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

                    // Optional: decide which requests need auth
                    sendWithoutRequest { request ->
                        request.url.pathSegments.none { it == "login" || it == "register" || it == "refresh" }
                    }
                }
            }
        }
    }
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
}
