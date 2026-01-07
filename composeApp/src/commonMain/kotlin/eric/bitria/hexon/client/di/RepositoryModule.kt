package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.repository.AuthRepository
import eric.bitria.hexon.client.repository.KtorAuthRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
}
