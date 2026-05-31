package eric.bitria.hexonkmp.data.storage

import kotlinx.browser.localStorage
import kotlin.random.Random

private const val KEY = "player_id"

actual fun createDevicePreferences(): DevicePreferences = object : DevicePreferences {
    override suspend fun getOrCreatePlayerId(): String =
        localStorage.getItem(KEY) ?: randomUuid().also { localStorage.setItem(KEY, it) }
}

private fun randomUuid(): String {
    val b = Random.nextBytes(16)
    b[6] = ((b[6].toInt() and 0x0f) or 0x40).toByte()
    b[8] = ((b[8].toInt() and 0x3f) or 0x80).toByte()
    return b.toHexString().let {
        "${it.substring(0,8)}-${it.substring(8,12)}-${it.substring(12,16)}-${it.substring(16,20)}-${it.substring(20)}"
    }
}

private fun ByteArray.toHexString() = joinToString("") {
    it.toInt().and(0xff).toString(16).padStart(2, '0')
}
