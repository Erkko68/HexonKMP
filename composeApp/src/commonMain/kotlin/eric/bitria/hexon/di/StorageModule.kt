package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import eric.bitria.hexon.api.TokenStore
import org.koin.dsl.module

/**
 * Common storage module.
 * The [persistRefreshToken] flag should be set to false for platforms where refresh tokens
 * are handled via cookies (like Web), and true where they should be manually stored (like Mobile).
 */
fun storageModule(persistRefreshToken: Boolean) = module {
    single { Settings() }
    single { TokenStore(get(), persistRefreshToken) }
}
