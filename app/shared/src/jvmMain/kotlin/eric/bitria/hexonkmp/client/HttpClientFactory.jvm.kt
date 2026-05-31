package eric.bitria.hexonkmp.client

import io.ktor.client.*
import io.ktor.client.engine.java.*

actual fun createHttpClient(): HttpClient = HttpClient(Java) { commonConfig() }
