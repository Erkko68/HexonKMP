package eric.bitria.hexonkmp.di

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.client.createHttpClient
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.repository.GameRepositoryImpl
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import eric.bitria.hexonkmp.data.storage.createDevicePreferences
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import eric.bitria.hexonkmp.ui.screens.lobby.LobbyViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun appModule() = module {
    single<DevicePreferences> { createDevicePreferences() }
    single { createHttpClient(get()) }
    single { GameClient(get()) }
    single<GameRepository> { GameRepositoryImpl(get()) }
    viewModelOf(::LobbyViewModel)
    viewModelOf(::GameViewModel)
}
