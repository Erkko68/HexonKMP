package eric.bitria.hexon.di

import eric.bitria.hexon.api.SessionManager
import eric.bitria.hexon.api.client.*
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.api.repository.AuthRepositoryImpl
import eric.bitria.hexon.api.repository.SocialRepository
import eric.bitria.hexon.api.repository.SocialRepositoryImpl
import eric.bitria.hexon.api.repository.UserRepository
import eric.bitria.hexon.api.repository.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    // Session Manager
    single { SessionManager({ get<AuthClient>() }, get()) }

    // Clients (API Layer)
    single<AuthClient> { KtorAuthClient(get()) }
    single<UserClient> { KtorUserClient(get()) }
    single<SocialClient> { KtorSocialClient(get()) }

    // Repositories (Domain Layer)
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get()) }
}
