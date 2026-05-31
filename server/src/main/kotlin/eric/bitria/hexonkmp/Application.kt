package eric.bitria.hexonkmp

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.routes.gameRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.websocket.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule())
    }

    install(CORS) {
        anyHost()                           // TODO restrict to your domain in production
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
    }

    install(ContentNegotiation) {
        json(AppJson)
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 60.seconds
    }

    gameRoutes()
}
