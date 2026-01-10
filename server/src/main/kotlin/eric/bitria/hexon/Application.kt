package eric.bitria.hexon

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.login.LoginServiceImpl
import eric.bitria.hexon.auth.refresh.RefreshServiceImpl
import eric.bitria.hexon.auth.register.RegisterServiceImpl
import eric.bitria.hexon.auth.repository.ExposedAuthRepository
import eric.bitria.hexon.auth.token.JwtConfig
import eric.bitria.hexon.auth.token.JwtTokenService
import eric.bitria.hexon.database.DatabaseFactory
import eric.bitria.hexon.email.repository.ExposedEmailVerificationRepository
import eric.bitria.hexon.email.smtp.SmtpConfig
import eric.bitria.hexon.email.smtp.SmtpServiceImp
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.users.account.UserAccountServiceImpl
import eric.bitria.hexon.users.profile.ExposedProfileRepository
import eric.bitria.hexon.users.profile.UserProfileServiceImpl
import eric.bitria.hexon.users.verify.AccountVerificationServiceImpl
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
    val emailVerificationRepository = ExposedEmailVerificationRepository()
    val authRepository = ExposedAuthRepository()
    val profileRepository = ExposedProfileRepository()

    // 3. Install plugins
    install(ContentNegotiation) { json() }

    // 4. Configure security (JWT)
    configureSecurity(jwtConfig)

    // 5. Initialize services
    val tokenService = JwtTokenService(jwtConfig)
    val emailService = SmtpServiceImp(smtpConfig)
    val emailVerificationService = EmailVerificationServiceImpl(
        verificationRepo = emailVerificationRepository,
        smtpService = emailService,
        authRepository = authRepository
    )
    val registerService = RegisterServiceImpl(
        authRepository = authRepository,
        emailVerificationService = emailVerificationService
    )
    val userProfileService = UserProfileServiceImpl(
        profileRepository = profileRepository
    )
    val accountVerificationService = AccountVerificationServiceImpl(
        profileRepository = profileRepository,
        authRepository = authRepository,
        emailVerificationService = emailVerificationService,
        tokenService = tokenService
    )
    val loginService = LoginServiceImpl(
        authRepository = authRepository,
        tokenService = tokenService
    )
    val refreshService = RefreshServiceImpl(
        authRepository = authRepository,
        tokenService = tokenService
    )
    val passwordService = UserAccountServiceImpl(
        emailVerificationService = emailVerificationService,
        authRepository = authRepository
    )

    // Routes
    routing {
        authRoutes(
            registerService = registerService,
            loginService = loginService,
            refreshService = refreshService
        )
        usersRoutes(
            accountVerificationService = accountVerificationService,
            userAccountService = passwordService,
            userProfileService = userProfileService
        )
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
