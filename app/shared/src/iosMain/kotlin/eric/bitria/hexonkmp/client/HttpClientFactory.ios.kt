package eric.bitria.hexonkmp.client

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) { commonConfig() }
