package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import eric.bitria.hexon.api.PersistentCookieStorage
import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.api.client.GameSocketClient
import eric.bitria.hexon.api.client.KtorAuthClient
import eric.bitria.hexon.api.client.KtorGameSocketClient
import eric.bitria.hexon.api.client.KtorMatchmakingClient
import eric.bitria.hexon.api.client.KtorSocialClient
import eric.bitria.hexon.api.client.KtorUserClient
import eric.bitria.hexon.api.client.MatchmakingClient
import eric.bitria.hexon.api.client.SocialClient
import eric.bitria.hexon.api.client.UserClient
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.api.repository.AuthRepositoryImpl
import eric.bitria.hexon.api.repository.GameRepository
import eric.bitria.hexon.api.repository.GameRepositoryImpl
import eric.bitria.hexon.api.repository.MatchmakingRepository
import eric.bitria.hexon.api.repository.MatchmakingRepositoryImpl
import eric.bitria.hexon.api.repository.SocialRepository
import eric.bitria.hexon.api.repository.SocialRepositoryImpl
import eric.bitria.hexon.api.repository.UserRepository
import eric.bitria.hexon.api.repository.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {

    // Network Headers / Cookies
    single { PersistentCookieStorage(get<Settings>()) }
    single { TokenStore(get()) }

    // Clients (API Layer)
    single<AuthClient> { KtorAuthClient(get()) }
    single<UserClient> { KtorUserClient(get()) }
    single<SocialClient> { KtorSocialClient(get()) }
    single<MatchmakingClient> { KtorMatchmakingClient(get()) }
    single<GameSocketClient> { KtorGameSocketClient(get()) }

    // Repositories (Domain Layer)
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get()) }
    single<MatchmakingRepository> { MatchmakingRepositoryImpl(get()) }
    single<GameRepository> { GameRepositoryImpl(get()) }
}
