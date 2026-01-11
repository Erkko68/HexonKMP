package eric.bitria.hexon.client.di

import com.russhwolf.settings.Settings
import eric.bitria.hexon.client.persistence.AccountManager
import eric.bitria.hexon.client.persistence.AccountManagerImpl
import eric.bitria.hexon.client.persistence.EncryptedData
import eric.bitria.hexon.client.persistence.EncryptedDataImpl
import eric.bitria.hexon.client.persistence.SettingsManager
import eric.bitria.hexon.client.persistence.SettingsManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformStorageModule: Module

val storageModule = module {
    // Multiplatform Settings (Standard)
    single { Settings() }
    single<SettingsManager> { SettingsManagerImpl(get()) }
    
    // Encrypted Data (KVault) - provided in platformStorageModule usually,
    single<EncryptedData> { EncryptedDataImpl(get()) }
    
    single<AccountManager> { AccountManagerImpl(get()) }
}
