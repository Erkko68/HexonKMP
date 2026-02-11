package eric.bitria.hexon.di

import eric.bitria.hexon.data.local.TokenStorage
import eric.bitria.hexon.data.local.WebTokenStorage
import eric.bitria.hexon.data.repository.InMemoryUserPreferencesRepository
import eric.bitria.hexon.data.repository.UserPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { WebTokenStorage() }
    single<UserPreferencesRepository> { InMemoryUserPreferencesRepository() }
}