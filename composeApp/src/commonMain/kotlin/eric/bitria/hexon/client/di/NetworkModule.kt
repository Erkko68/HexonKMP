package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.auth.SessionManager
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
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        val tokenManager = get<TokenManager>()
        val platform = get<TargetPlatform>()

        HttpClient {
            // 1. Defaults: Set the Base URL so you don't repeat it everywhere
            install(DefaultRequest) {
                url(platform.baseUrl)
                contentType(ContentType.Application.Json) // Default to JSON
            }

            // 2. Logging: For debugging Auth flows
            //install(Logging) {
            //    logger = Logger.SIMPLE
            //    level = LogLevel.ALL // Switch to INFO or HEADERS in production
            //}

            // 3. Serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }

            // 4. Authentication
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

                        try {
                            val response = client.post("/auth/refresh") {
                                markAsRefreshTokenRequest()
                                setBody(RefreshRequest(refreshToken))
                            }.body<RefreshResponse>()

                            if (response.result == RefreshResult.SUCCESS) {
                                val newAccess = response.accessToken!!
                                val newRefresh = response.refreshToken!!

                                tokenManager.saveTokens(newAccess, newRefresh)
                                BearerTokens(newAccess, newRefresh)
                            } else {
                                SessionManager.logout()
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath

                        // Returns true if it starts with /auth (e.g. /auth/login)
                        val isAuth = path.startsWith("/auth")

                        // Returns true if it is a verification endpoint
                        val isVerification = path.startsWith("/users/email")

                        // If it is NOT public, we send the token
                        !(isAuth || isVerification)
                    }
                }
            }
        }
    }
}