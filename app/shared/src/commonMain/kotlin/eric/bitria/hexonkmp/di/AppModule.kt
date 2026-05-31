package eric.bitria.hexonkmp.di

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.client.createHttpClient
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.repository.GameRepositoryImpl
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val BASE_URL = "http://localhost:8080"

fun appModule() = module {
    single { createHttpClient() }
    single { GameClient(get(), BASE_URL) }
    single<GameRepository> { GameRepositoryImpl(get()) }
    viewModelOf(::GameViewModel)
}
