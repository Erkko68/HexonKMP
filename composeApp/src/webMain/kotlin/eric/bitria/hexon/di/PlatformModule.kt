package eric.bitria.hexon.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { WebTokenStorage() }
}