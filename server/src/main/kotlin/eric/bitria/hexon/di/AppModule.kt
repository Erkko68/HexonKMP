package eric.bitria.hexon.di

import eric.bitria.hexon.auth.login.LoginService
import eric.bitria.hexon.auth.login.LoginServiceImpl
import eric.bitria.hexon.auth.refresh.RefreshService
import eric.bitria.hexon.auth.refresh.RefreshServiceImpl
import eric.bitria.hexon.auth.register.RegisterService
import eric.bitria.hexon.auth.register.RegisterServiceImpl
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.repository.ExposedAuthRepository
import eric.bitria.hexon.auth.token.JwtConfig
import eric.bitria.hexon.auth.token.JwtTokenService
import eric.bitria.hexon.auth.token.TokenService
import eric.bitria.hexon.email.repository.EmailVerificationRepository
import eric.bitria.hexon.email.repository.ExposedEmailVerificationRepository
import eric.bitria.hexon.email.smtp.SmtpConfig
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.email.smtp.SmtpServiceImp
import eric.bitria.hexon.email.verification.EmailVerificationService
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.social.SocialService
import eric.bitria.hexon.social.SocialServiceImpl
import eric.bitria.hexon.social.repository.ExposedFriendRequestRepository
import eric.bitria.hexon.social.repository.ExposedFriendsRepository
import eric.bitria.hexon.social.repository.FriendRequestRepository
import eric.bitria.hexon.social.repository.FriendsRepository
import eric.bitria.hexon.users.account.UserAccountService
import eric.bitria.hexon.users.account.UserAccountServiceImpl
import eric.bitria.hexon.users.profile.ExposedProfileRepository
import eric.bitria.hexon.users.profile.ProfileRepository
import eric.bitria.hexon.users.profile.UserProfileService
import eric.bitria.hexon.users.profile.UserProfileServiceImpl
import eric.bitria.hexon.users.verify.AccountVerificationService
import eric.bitria.hexon.users.verify.AccountVerificationServiceImpl
import io.ktor.server.config.*
import org.koin.dsl.module

fun appModule(config: ApplicationConfig) = module {
    // Configuration
    single { JwtConfig.fromConfig(config) }
    single { SmtpConfig.fromConfig(config) }

    // Repositories
    single<EmailVerificationRepository> { ExposedEmailVerificationRepository() }
    single<AuthRepository> { ExposedAuthRepository() }
    single<ProfileRepository> { ExposedProfileRepository() }
    single<FriendsRepository> { ExposedFriendsRepository() }
    single<FriendRequestRepository> { ExposedFriendRequestRepository() }

    // Services
    single<TokenService> { JwtTokenService(get()) }
    single<SmtpService> { SmtpServiceImp(get()) }
    single<EmailVerificationService> { EmailVerificationServiceImpl(get(), get(), get()) }
    single<RegisterService> { RegisterServiceImpl(get(), get()) }
    single<UserProfileService> { UserProfileServiceImpl(get()) }
    single<AccountVerificationService> { AccountVerificationServiceImpl(get(), get(), get(), get()) }
    single<LoginService> { LoginServiceImpl(get(), get()) }
    single<RefreshService> { RefreshServiceImpl(get(), get()) }
    single<UserAccountService> { UserAccountServiceImpl(get(), get()) }
    single<SocialService> { SocialServiceImpl(get(), get(), get()) }
}
