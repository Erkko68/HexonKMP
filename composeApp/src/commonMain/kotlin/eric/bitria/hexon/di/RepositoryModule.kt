package eric.bitria.hexon.di

import eric.bitria.hexon.api.client.*
import eric.bitria.hexon.ui.repository.*
import org.koin.dsl.module

val repositoryModule = module {
    // Session Manager
    single { SessionManager({ get<AuthClient>() }, get(), get()) }

    // Clients (API Layer)
    single<AuthClient> { KtorAuthClient(get()) }
    single<UserClient> { KtorUserClient(get()) }
    single<SocialClient> { KtorSocialClient(get()) }

    // Repositories (Domain Layer)
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get()) }
}
