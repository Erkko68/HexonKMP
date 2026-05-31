package eric.bitria.hexonkmp.core.config

expect object EnvConfig {
    val SERVER_HOST: String
    val SERVER_PORT: Int
}

val EnvConfig.BASE_URL: String get() = "http://$SERVER_HOST:$SERVER_PORT"
val EnvConfig.WS_URL: String get() = "ws://$SERVER_HOST:$SERVER_PORT"
