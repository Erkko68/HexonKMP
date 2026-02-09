@file:JvmName("CommonNetworkModule")
package eric.bitria.hexon.di

import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.PersistentCookieStorage
import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.dtos.auth.RefreshResult
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.jvm.JvmName

expect fun HttpClientConfig<*>.configurePlatformCookies(cookieStorage: PersistentCookieStorage)

val networkModule = module {

    single {
        val tokenStore: TokenStore by inject()
        val authClient: AuthClient by inject()
        val cookieStorage: PersistentCookieStorage by inject()

        HttpClient {
            install(WebSockets)

            configurePlatformCookies(cookieStorage)

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
                        val response = authClient.refresh()

                        if (response.result == RefreshResult.SUCCESS && response.accessToken != null) {
                            tokenStore.save(response.accessToken!!)
                            BearerTokens(response.accessToken!!, "")
                        } else {
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
