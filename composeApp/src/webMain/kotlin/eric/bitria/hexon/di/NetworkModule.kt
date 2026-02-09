package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

actual val networkModule = module {
    single {
        val tokenStorage: TokenStorage by inject()
        val authClient: AuthClient by inject()

        HttpClient(Js) {
            install(WebSockets)
            install(HttpCookies) // Browser handles cookies automatically

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
                        // We just call refresh without a body (or with dummy data if the API requires it)
                        // and the browser will attach the cookie.
                        val response = authClient.refresh(RefreshRequest(""))

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
                        path.startsWith("/auth") || path.startsWith("/users/email")
                    }
                }
            }
        }
    }
}
