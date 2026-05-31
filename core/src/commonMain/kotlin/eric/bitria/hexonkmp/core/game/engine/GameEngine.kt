package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.board.BoardGenerator
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import kotlin.random.Random

// The pure game engine. Every rule lives behind reduce(); it has no concept of
// sockets, coroutines, or connected players. Same code runs on the server (as
// the source of truth) and on the client (to pre-validate before sending).
interface GameEngine {
    fun initialState(players: List<PlayerId>): GameState

    // (state, who acted, what they did) -> result. Must be a pure function:
    // no I/O, no shared mutable state, deterministic.
    fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult
}

// Generic Catan engine driven by a ScenarioConfig — swap the config to get a
// different game mode without touching this class. `boardSeed` makes board
// generation deterministic (override in tests for a fixed layout).
class CatanGameEngine(
    private val config: ScenarioConfig = ClassicCatan,
    private val boardSeed: Long = Random.nextLong(),
) : GameEngine {

    override fun initialState(players: List<PlayerId>): GameState =
        GameState(
            players = players,
            config = config,
            board = BoardGenerator.generate(config, boardSeed),
        )

    override fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult =
        when (action) {
            EndTurn -> endTurn(state, actor)
        }

    private fun endTurn(state: GameState, actor: PlayerId): GameResult {
        if (actor != state.currentPlayer) {
            return GameResult(state, rejection = "It is not your turn")
        }
        val nextIndex = (state.currentPlayerIndex + 1) % state.players.size
        val nextTurn = if (nextIndex == 0) state.turn + 1 else state.turn
        val next = state.copy(currentPlayerIndex = nextIndex, turn = nextTurn)
        return GameResult(next, events = listOf(TurnChanged(next.currentPlayer, next.turn)))
    }
}
