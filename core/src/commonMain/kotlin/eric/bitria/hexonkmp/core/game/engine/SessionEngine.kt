package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId

// The slice of the engine the TRANSPORT layer (server GameSession) needs: drive
// state forward from actions and presence changes. Deliberately free of any
// Catan-specific queries (legal moves, affordability) — those live on GameEngine
// and are used only by the client UI. Splitting them keeps GameSession depending
// on the smallest possible surface, documenting that it speaks only "advance the
// game", never "what are Catan's legal settlements".
//
// The returned GameState is Redactable, which is the other half of what the
// transport relies on (it ships per-recipient redacted snapshots/events).
interface SessionEngine {
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
