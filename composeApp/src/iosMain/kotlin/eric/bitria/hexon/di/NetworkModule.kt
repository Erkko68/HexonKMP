package eric.bitria.hexon.di

import eric.bitria.hexon.api.PersistentCookieStorage
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.cookies.HttpCookies

actual fun HttpClientConfig<*>.configurePlatformCookies(cookieStorage: PersistentCookieStorage) {
    install(HttpCookies) {
        storage = cookieStorage
    }
}
