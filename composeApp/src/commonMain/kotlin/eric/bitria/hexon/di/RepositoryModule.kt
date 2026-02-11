package eric.bitria.hexon.di

import eric.bitria.hexon.data.remote.AssetsClient
import eric.bitria.hexon.data.remote.AuthClient
import eric.bitria.hexon.data.remote.GameSocketClient
import eric.bitria.hexon.data.remote.KtorAssetsClient
import eric.bitria.hexon.data.remote.KtorAuthClient
import eric.bitria.hexon.data.remote.KtorGameSocketClient
import eric.bitria.hexon.data.remote.KtorMatchmakingClient
import eric.bitria.hexon.data.remote.KtorSocialClient
import eric.bitria.hexon.data.remote.KtorUserClient
import eric.bitria.hexon.data.remote.MatchmakingClient
import eric.bitria.hexon.data.remote.SocialClient
import eric.bitria.hexon.data.remote.UserClient
import eric.bitria.hexon.data.repository.AssetsRepository
import eric.bitria.hexon.data.repository.AssetsRepositoryImpl
import eric.bitria.hexon.data.repository.AuthRepository
import eric.bitria.hexon.data.repository.AuthRepositoryImpl
import eric.bitria.hexon.data.repository.GameRepository
import eric.bitria.hexon.data.repository.GameRepositoryImpl
import eric.bitria.hexon.data.repository.MatchmakingRepository
import eric.bitria.hexon.data.repository.MatchmakingRepositoryImpl
import eric.bitria.hexon.data.repository.SocialRepository
import eric.bitria.hexon.data.repository.SocialRepositoryImpl
import eric.bitria.hexon.data.repository.UserRepository
import eric.bitria.hexon.data.repository.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {

    // Clients (API Layer)
    single<AuthClient> { KtorAuthClient(get()) }
    single<UserClient> { KtorUserClient(get()) }
    single<SocialClient> { KtorSocialClient(get()) }
    single<MatchmakingClient> { KtorMatchmakingClient(get()) }
    single<GameSocketClient> { KtorGameSocketClient(get()) }
    single<AssetsClient> { KtorAssetsClient(get()) }

    // Repositories (Domain Layer)
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get()) }
    single<MatchmakingRepository> { MatchmakingRepositoryImpl(get()) }
    single<GameRepository> { GameRepositoryImpl(get()) }
    single<AssetsRepository> { AssetsRepositoryImpl(get()) }
}
