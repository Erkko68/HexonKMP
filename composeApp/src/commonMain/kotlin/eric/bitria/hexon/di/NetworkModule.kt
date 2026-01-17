package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.SessionManager
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
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        
        HttpClient {
            install(DefaultRequest) {
                url(BuildKonfig.BASE_URL)
                contentType(ContentType.Application.Json)
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
                        // Resolve sessionManager lazily inside the lambda
                        val sessionManager = get<SessionManager>()
                        val accessToken = sessionManager.getAccessToken()
                        val refreshToken = sessionManager.getRefreshToken()
                        BearerTokens(accessToken, refreshToken)
                    }

                    refreshTokens {
                        val sessionManager = get<SessionManager>()
                        val refreshToken = sessionManager.getRefreshToken()

                        try {
                            val response = client.post("/auth/refresh") {
                                markAsRefreshTokenRequest()
                                setBody(RefreshRequest(refreshToken))
                            }.body<RefreshResponse>()

                            if (response.result == RefreshResult.SUCCESS) {
                                val newAccess = response.accessToken!!
                                val newRefresh = response.refreshToken!!

                                sessionManager.saveTokens(newAccess, newRefresh)
                                BearerTokens(newAccess, newRefresh)
                            } else {
                                sessionManager.logout()
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        val isAuth = path.startsWith("/auth")
                        val isVerification = path.startsWith("/users/email")
                        !(isAuth || isVerification)
                    }
                }
            }
        }
    }
}
