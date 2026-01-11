package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.AuthClient
import eric.bitria.hexon.client.KtorAuthClient
import eric.bitria.hexon.client.KtorUserClient
import eric.bitria.hexon.client.SessionManager
import eric.bitria.hexon.client.UserClient
import eric.bitria.hexon.ui.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { SessionManager({ get<AuthClient>() }, get(), get()) }
    single<AuthClient> { KtorAuthClient(get(), get()) }
    single<UserClient> { KtorUserClient(get(), get()) }
    single { UserRepository(get()) }
}
