package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.PersistentCookieStorage
import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
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

val networkModule = module {

    single {
        val cookieStorage: PersistentCookieStorage by inject()
        val tokenStore: TokenStore by inject()
        val authClient: AuthClient by inject()

        HttpClient {
            install(WebSockets)

            install(HttpCookies) {
                storage = cookieStorage
            }

            install(DefaultRequest) {
                url(BuildKonfig.BASE_URL)
                contentType(ContentType.Application.Json)
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        tokenStore.get()?.let { BearerTokens(it, "") }
                    }

                    refreshTokens {
                        // 1. Make the raw API call
                        val response = authClient.refresh()

                        // 2. Check if successful
                        if (response.result == RefreshResult.SUCCESS && response.accessToken != null) {
                            // Store Token
                            tokenStore.save(response.accessToken!!)
                            // Return the NEW token to Ktor so it can retry the request
                            BearerTokens(response.accessToken!!, "")
                        } else {
                            // If refresh fails, clear the local store to force a logout state
                            tokenStore.clear()
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
