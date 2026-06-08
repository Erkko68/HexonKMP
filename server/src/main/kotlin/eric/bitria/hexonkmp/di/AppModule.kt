package eric.bitria.hexonkmp.di

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.auth.TokenRegistry
import eric.bitria.hexonkmp.core.protocol.CatanCodec
import eric.bitria.hexonkmp.repository.GameSessionRepository
import eric.bitria.hexonkmp.repository.InMemoryGameSessionRepository
import eric.bitria.hexonkmp.session.GameSession
import org.koin.dsl.module

// The only place that names the concrete game: it binds the Catan engine, codec,
// and match config into a session factory. The transport (repository, session,
// routes) is generic and unaware of Catan — swap this wiring to host another game.
fun appModule() = module {
    single { TokenRegistry() }
    single<GameSessionRepository<GameState, GameAction, GameEvent>> {
        val config = ClassicCatan
        InMemoryGameSessionRepository { gameId, manualStart, onEmpty ->
            GameSession(
                gameId = gameId,
                engine = CatanGameEngine(config),
                codec = CatanCodec,
                minPlayers = config.rules.minPlayers,
                maxPlayers = config.rules.maxPlayers,
                autoStartDelaySeconds = config.rules.autoStartDelaySeconds,
                manualStart = manualStart,
                onEmpty = onEmpty,
            )
        }
    }
}
