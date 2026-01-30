package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import eric.bitria.hexon.BuildKonfig
import eric.bitria.hexon.api.PersistentCookieStorage
import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.api.client.KtorAuthClient
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.AuthRepository
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

    // 1. Storage Components (Data Layer)
    single { PersistentCookieStorage(get<Settings>()) }
    single { TokenStore(get()) }

    // 2. Auth Client wrapper
    single<AuthClient> { KtorAuthClient(get()) }

    // 3. The HttpClient
    single {
        HttpClient {
            install(WebSockets)

            // A. ENABLE PERSISTENT COOKIES
            install(HttpCookies) {
                storage = get<PersistentCookieStorage>()
            }

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

            // B. AUTHENTICATION LOGIC
            install(Auth) {
                bearer {
                    // 1. Load Token from RAM (for normal requests)
                    loadTokens {
                        val tokenStore = get<TokenStore>()
                        val token = tokenStore.get()
                        if (token != null) BearerTokens(token, "") else null
                    }

                    // 2. Refresh Logic (for 401 errors)
                    refreshTokens {
                        // LAZY INJECTION: Break circular dependency
                        // HttpClient -> AuthRepository -> AuthClient -> HttpClient
                        val repo = get<AuthRepository>()

                        // Delegate to Repository
                        val result = repo.refresh()

                        if (result is ApiResult.Success && result.data == RefreshResult.SUCCESS) {
                            // Repository already updated TokenStore, just read it back
                            val newToken = get<TokenStore>().get()
                            BearerTokens(newToken!!, "")
                        } else {
                            null
                        }
                    }

                    // Exclude public endpoints from authentication
                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        path.startsWith("/auth") || path.startsWith("/users/email")
                    }
                }
            }
        }
    }
}