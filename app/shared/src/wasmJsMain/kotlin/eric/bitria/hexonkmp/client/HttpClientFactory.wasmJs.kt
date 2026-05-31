package eric.bitria.hexonkmp.client

import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createHttpClient(): HttpClient = HttpClient(Js) { commonConfig() }
