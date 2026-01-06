package eric.bitria.hexon.auth.email

import io.ktor.server.config.ApplicationConfig

data class SmtpConfig(
    val smtpUser: String,
    val smtpPassword: String,
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: String = "587"
) {
    companion object {
        fun fromConfig(config: ApplicationConfig): SmtpConfig {
            return SmtpConfig(
                smtpUser = config.property("smtp.user").getString(),
                smtpPassword = config.property("smtp.password").getString(),
                smtpHost = config.propertyOrNull("smtp.host")?.getString() ?: "smtp.gmail.com",
                smtpPort = config.propertyOrNull("smtp.port")?.getString() ?: "587"
            )
        }
    }
}
