package eric.bitria.auth

import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.routes.registerRoutes
import eric.bitria.hexon.Greeting
import eric.bitria.hexon.SERVER_PORT
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    //configureAuthRoutes(RegisterService())

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureAuthRoutes(
    registerService: RegisterService,
    //loginService: LoginService
) {
    routing {
        registerRoutes(registerService)
        //loginRoutes(loginService)
    }
}
