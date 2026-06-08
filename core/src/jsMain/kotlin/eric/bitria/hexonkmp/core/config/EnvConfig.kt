package eric.bitria.hexonkmp.core.config

import kotlinx.browser.window

actual object EnvConfig {
    actual val SERVER_HOST: String = window.location.hostname
    actual val SERVER_PORT: Int = 8081
    actual val SECURE: Boolean = window.location.protocol == "https:"
}
