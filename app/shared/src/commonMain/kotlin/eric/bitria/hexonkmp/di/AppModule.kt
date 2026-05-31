package eric.bitria.hexonkmp.di

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.client.createHttpClient
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.repository.GameRepositoryImpl
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import eric.bitria.hexonkmp.data.storage.createDevicePreferences
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun appModule() = module {
    single { createHttpClient() }
    single { GameClient(get()) }
    single<GameRepository> { GameRepositoryImpl(get()) }
    single<DevicePreferences> { createDevicePreferences() }
    viewModelOf(::GameViewModel)
}
