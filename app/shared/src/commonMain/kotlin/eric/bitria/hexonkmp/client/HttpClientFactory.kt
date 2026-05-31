package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.config.EnvConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

expect fun createHttpClient(): HttpClient

fun HttpClientConfig<*>.commonConfig() {
    install(ContentNegotiation) { json(AppJson) }
    install(WebSockets)
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 10_000
    }
    defaultRequest {
        host = EnvConfig.SERVER_HOST
        port = EnvConfig.SERVER_PORT
        contentType(ContentType.Application.Json)
    }
}
