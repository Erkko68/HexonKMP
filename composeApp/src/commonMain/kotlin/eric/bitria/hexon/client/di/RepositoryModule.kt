package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.repository.AuthClient
import eric.bitria.hexon.client.repository.KtorAuthClient
import eric.bitria.hexon.client.repository.KtorUserClient
import eric.bitria.hexon.client.repository.UserClient
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthClient> { KtorAuthClient(get(), get()) }
    single<UserClient> { KtorUserClient(get()) }
}
