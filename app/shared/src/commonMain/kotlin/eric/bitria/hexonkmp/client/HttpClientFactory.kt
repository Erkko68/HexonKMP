package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.AppJson
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*

expect fun createHttpClient(): HttpClient

fun HttpClientConfig<*>.commonConfig() {
    install(ContentNegotiation) { json(AppJson) }
    install(WebSockets)
}
