package eric.bitria.hexon.di

import eric.bitria.hexon.data.InMemoryUserPreferencesRepository
import eric.bitria.hexon.data.UserPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { WebTokenStorage() }
    single<UserPreferencesRepository> { InMemoryUserPreferencesRepository() }
}