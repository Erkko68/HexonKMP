package eric.bitria.hexon.di

import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.api.client.KtorAuthClient
import eric.bitria.hexon.api.client.KtorSocialClient
import eric.bitria.hexon.api.client.KtorUserClient
import eric.bitria.hexon.api.client.SessionManager
import eric.bitria.hexon.api.client.SocialClient
import eric.bitria.hexon.api.client.UserClient
import eric.bitria.hexon.ui.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { SessionManager({ get<AuthClient>() }, get(), get()) }
    single<AuthClient> { KtorAuthClient(get(), get()) }
    single<UserClient> { KtorUserClient(get(), get()) }
    single<SocialClient> { KtorSocialClient(get()) }
    single { UserRepository(get()) }
}
