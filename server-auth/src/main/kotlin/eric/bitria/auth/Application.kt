package eric.bitria.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.auth.register.RegisterRepositoryDB
import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.routes.registerRoutes
import eric.bitria.auth.token.JwtConfig
import eric.bitria.auth.token.JwtTokenService
import eric.bitria.hexon.SERVER_PORT
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Install plugins
    install(ContentNegotiation) { json() }

    // 2. Configure security
    val jwtConfig = JwtConfig(
        issuer = "hexon",
        audience = "hexon-users",
        secret = System.getenv("JWT_SECRET"),
        accessTokenTtlMillis = 15 * 60 * 1000,
        refreshTokenTtlMillis = 30L * 24 * 60 * 60 * 1000
    )
    configureSecurity(jwtConfig)

    // 3. Initialize services
    val jwtService = JwtTokenService(jwtConfig)

    val registerService = RegisterService(
        repository = RegisterRepositoryDB(),
        tokenService = jwtService
    )
    // val loginService = LoginService(...)

    // Routes
    routing {
        registerRoutes(registerService)
        // loginRoutes(loginService)
    }
}

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    authentication {
        jwt("auth-jwt") {
            realm = "hexon"

            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtConfig.secret))
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            )

            validate { credential ->
                if (credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}