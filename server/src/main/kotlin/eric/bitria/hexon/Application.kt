package eric.bitria.hexon

import eric.bitria.hexon.database.DatabaseFactory
import eric.bitria.hexon.di.appModule
import eric.bitria.hexon.routes.assetsRoutes
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.routes.matchmakingRoutes
import eric.bitria.hexon.routes.socialRoutes
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.security.configureSecurity
import eric.bitria.hexon.security.configureSessions
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // 1. Initialize Koin
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment.config))
    }

    // 2. Initialize Database
    DatabaseFactory.init(environment.config)

    // 3. Install plugins
    install(ContentNegotiation) { json() }
    configureSessions()
    configureSecurity()

    install(WebSockets)

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        
        allowCredentials = true
        allowHost("localhost:8081")
        allowHost("192.168.100.254:8081")
    }

    // 5. Routes
    routing {
        authRoutes()
        usersRoutes()
        socialRoutes()
        matchmakingRoutes()
        assetsRoutes()
    }
}
