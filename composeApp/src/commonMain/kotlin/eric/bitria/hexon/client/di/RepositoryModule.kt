package eric.bitria.hexon.client.di

import eric.bitria.hexon.client.repository.AccountRepository
import eric.bitria.hexon.client.repository.AuthRepository
import eric.bitria.hexon.client.repository.KtorAccountRepository
import eric.bitria.hexon.client.repository.KtorAuthRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
    single<AccountRepository> { KtorAccountRepository(get()) }
}
