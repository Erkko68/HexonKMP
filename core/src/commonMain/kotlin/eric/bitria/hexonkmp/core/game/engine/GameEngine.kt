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

    // Presence changes. A player leaving while it is their turn must hand the
    // turn to the next present player, otherwise the game stalls. A player
    // rejoining is simply marked present again — the turn order is unchanged.
    fun playerLeft(state: GameState, playerId: PlayerId): GameResult
    fun playerJoined(state: GameState, playerId: PlayerId): GameResult
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
            present = players.toSet(),
            config = config,
            board = BoardGenerator.generate(config, boardSeed),
        )

    override fun reduce(state: GameState, actor: PlayerId, action: GameAction): GameResult =
        when (action) {
            EndTurn -> endTurn(state, actor)
        }

    override fun playerLeft(state: GameState, playerId: PlayerId): GameResult {
        if (playerId !in state.present) return GameResult(state)
        val base = state.copy(present = state.present - playerId)
        // If the player whose turn it is leaves, pass the turn along so the
        // remaining players can keep going.
        return if (state.currentPlayer == playerId) advanceTurn(base) else GameResult(base)
    }

    override fun playerJoined(state: GameState, playerId: PlayerId): GameResult {
        if (playerId !in state.players || playerId in state.present) return GameResult(state)
        return GameResult(state.copy(present = state.present + playerId))
    }

    private fun endTurn(state: GameState, actor: PlayerId): GameResult {
        if (actor != state.currentPlayer) {
            return GameResult(state, rejection = "It is not your turn")
        }
        return advanceTurn(state)
    }

    // Moves the turn to the next *present* player after the current index,
    // skipping anyone who has left. Increments the turn counter when the seating
    // wraps past the end. No-op if nobody else is present.
    private fun advanceTurn(state: GameState): GameResult {
        val size = state.players.size
        for (step in 1..size) {
            val candidate = (state.currentPlayerIndex + step) % size
            if (state.players[candidate] in state.present) {
                val wrapped = state.currentPlayerIndex + step >= size
                val nextTurn = if (wrapped) state.turn + 1 else state.turn
                val next = state.copy(currentPlayerIndex = candidate, turn = nextTurn)
                return GameResult(next, events = listOf(TurnChanged(next.currentPlayer, next.turn)))
            }
        }
        // Only the current player (or nobody) is present — nothing to advance to.
        return GameResult(state)
    }
}
