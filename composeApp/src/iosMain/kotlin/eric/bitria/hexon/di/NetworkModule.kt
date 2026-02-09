package eric.bitria.hexon.di

import io.ktor.client.HttpClientConfig

actual fun HttpClientConfig<*>.configurePlatformNetworking() {
    // No specific configuration for iOS (HttpCookies not installed)
}
