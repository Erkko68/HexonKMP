package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.AuthClient
import eric.bitria.hexon.client.KtorAuthClient
import eric.bitria.hexon.client.KtorUserClient
import eric.bitria.hexon.client.UserClient
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthClient> { KtorAuthClient(get(), get()) }
    single<UserClient> { KtorUserClient(get()) }
}
