package eric.bitria.hexonkmp.di

import eric.bitria.hexonkmp.repository.GameSessionRepository
import eric.bitria.hexonkmp.repository.InMemoryGameSessionRepository
import org.koin.dsl.module

fun appModule() = module {
    single<GameSessionRepository> { InMemoryGameSessionRepository() }
}
