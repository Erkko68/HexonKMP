package eric.bitria.hexon.di

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.cookies.HttpCookies

actual fun HttpClientConfig<*>.configurePlatformNetworking() {
    install(HttpCookies)
}
