package eric.bitria.hexon

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.repository.AuthRepositoryDB
import eric.bitria.hexon.database.DatabaseFactory
import eric.bitria.hexon.auth.email.SmtpConfig
import eric.bitria.hexon.auth.email.SmtpEmailService
import eric.bitria.hexon.auth.login.LoginServiceImp
import eric.bitria.hexon.auth.password.PasswordServiceImp
import eric.bitria.hexon.auth.refresh.RefreshServiceImp
import eric.bitria.hexon.auth.register.RegisterServiceImp
import eric.bitria.hexon.routes.loginRoute
import eric.bitria.hexon.routes.refreshRoute
import eric.bitria.hexon.routes.registerRoutes
import eric.bitria.hexon.auth.token.JwtConfig
import eric.bitria.hexon.auth.token.JwtTokenService
import eric.bitria.hexon.routes.passwordRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // 1. Load Configuration
    val jwtConfig = JwtConfig.fromConfig(environment.config)
    val smtpConfig = SmtpConfig.fromConfig(environment.config)

    // 2. Initialize Database
    DatabaseFactory.init(environment.config)
    val authRepository = AuthRepositoryDB()

    // 3. Install plugins
    install(ContentNegotiation) { json() }

    // 4. Configure security (JWT)
    configureSecurity(jwtConfig)

    // 5. Initialize services
    val jwtService = JwtTokenService(jwtConfig)
    val emailService = SmtpEmailService(smtpConfig)
    val passwordService = PasswordServiceImp(
        emailService = emailService,
        tokenService = jwtService
    )

    val registerService = RegisterServiceImp(
        repository = authRepository,
        tokenService = jwtService,
        emailService = emailService
    )

    val loginService = LoginServiceImp(
        repository = authRepository,
        tokenService = jwtService,
        emailService = emailService
    )

    val refreshService = RefreshServiceImp(jwtService)

    // Routes
    routing {
        registerRoutes(registerService)
        loginRoute(loginService)
        refreshRoute(refreshService)
        passwordRoutes(passwordService)
    }
}

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    authentication {
        jwt("auth-jwt") {
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
