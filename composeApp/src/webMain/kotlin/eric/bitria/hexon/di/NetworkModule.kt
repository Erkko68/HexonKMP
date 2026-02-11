package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
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
        val tokenStorage: TokenStorage = get()

        HttpClient(Js) {
            install(WebSockets)
            install(HttpCookies)

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
                        if (access != null) {
                            BearerTokens(access, "")
                        } else null
                    }

                    refreshTokens {
                        // For web, the refresh token is in an HttpOnly cookie.
                        val response: RefreshResponse = client.post("/auth/refresh") {
                            setBody(RefreshRequest(""))
                            markAsRefreshTokenRequest()
                        }.body()

                        if (response.result == RefreshResult.SUCCESS && response.accessToken != null) {
                            tokenStorage.saveAccess(response.accessToken!!)
                            BearerTokens(response.accessToken!!, "")
                        } else {
                            tokenStorage.clear()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        // Proactively send token for all requests except initial auth endpoints to avoid 401 challenges
                        !path.endsWith("/auth/login") &&
                        !path.endsWith("/auth/register") &&
                        !path.endsWith("/auth/refresh")
                    }
                }
            }
        }
    }
}
