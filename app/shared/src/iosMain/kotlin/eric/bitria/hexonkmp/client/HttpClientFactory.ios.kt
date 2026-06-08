package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.data.storage.DevicePreferences
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(prefs: DevicePreferences): HttpClient = HttpClient(Darwin) { commonConfig(prefs) }
