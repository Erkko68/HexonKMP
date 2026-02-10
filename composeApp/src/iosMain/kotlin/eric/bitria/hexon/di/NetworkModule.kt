package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

actual val networkModule = module {
    single {
        val tokenStorage: TokenStorage by inject()

        HttpClient(Darwin) {
            install(WebSockets)

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
                        val access = tokenStorage.getAccess()
                        val refresh = tokenStorage.getRefresh()
                        if (access != null && refresh != null) {
                            BearerTokens(access, refresh)
                        } else null
                    }

                    refreshTokens {
                        val refreshToken = tokenStorage.getRefresh() ?: return@refreshTokens null
                        val response: RefreshResponse = client.post("/auth/refresh") {
                            setBody(RefreshRequest(refreshToken))
                            markAsRefreshTokenRequest()
                        }.body()

                        if (response.result == RefreshResult.SUCCESS && response.accessToken != null) {
                            tokenStorage.saveAccess(response.accessToken!!)
                            response.refreshToken?.let { tokenStorage.saveRefresh(it) }
                            BearerTokens(response.accessToken!!, response.refreshToken ?: refreshToken)
                        } else {
                            tokenStorage.clear()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        path.startsWith("/auth") || path.startsWith("/users/email")
                    }
                }
            }
        }
    }
}
