package eric.bitria.hexonkmp.core.config

expect object EnvConfig {
    val SERVER_HOST: String
    val SERVER_PORT: Int

    // Whether to connect over TLS (https/wss). On web this follows the page's own
    // protocol so an HTTPS-served bundle never makes a blocked mixed-content call.
    val SECURE: Boolean
}

val EnvConfig.BASE_URL: String get() = "${if (SECURE) "https" else "http"}://$SERVER_HOST:$SERVER_PORT"
val EnvConfig.WS_URL: String get() = "${if (SECURE) "wss" else "ws"}://$SERVER_HOST:$SERVER_PORT"
