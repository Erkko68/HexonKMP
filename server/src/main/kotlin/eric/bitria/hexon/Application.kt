package eric.bitria.hexon

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.token.JwtConfig
import eric.bitria.hexon.database.DatabaseFactory
import eric.bitria.hexon.di.appModule
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.routes.socialRoutes
import eric.bitria.hexon.routes.usersRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
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

    // 4. Configure security (JWT)
    val jwtConfig by inject<JwtConfig>()
    configureSecurity(jwtConfig)

    // 5. Routes
    routing {
        authRoutes()
        usersRoutes()
        socialRoutes()
    }
}

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    authentication {
        jwt {
            realm = jwtConfig.realm

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
