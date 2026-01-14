package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import org.koin.dsl.module

val storageModule = module {
    // Multiplatform Settings (Standard)
    single { Settings() }
}
