package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<Settings> { SharedPreferencesSettings(get()) }
    single<TokenStorage> { MobileTokenStorage(get()) }
}