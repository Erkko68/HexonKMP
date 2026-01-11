package eric.bitria.hexon.client.di

import com.russhwolf.settings.Settings
import eric.bitria.hexon.client.persistence.EncryptedData
import eric.bitria.hexon.client.persistence.EncryptedDataImpl
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformStorageModule: Module

val storageModule = module {
    // Multiplatform Settings (Standard)
    single { Settings() }
    
    // Encrypted Data (KVault) - provided in platformStorageModule usually,
    single<EncryptedData> { EncryptedDataImpl(get()) }
}
