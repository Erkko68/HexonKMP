package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.data.storage.DevicePreferences
import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createHttpClient(prefs: DevicePreferences): HttpClient = HttpClient(Js) { commonConfig(prefs) }
