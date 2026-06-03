package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.model.PlayerId

// The game-agnostic engine contract the TRANSPORT layer drives: advance a game
// from actions and presence changes. Generic over a game's state [S], action [A],
// and event [E] types — so the server (GameSession), matchmaking, and protocol
// can host ANY turn-based game, not just Catan. The concrete rules (and any
// game-specific queries) live in an implementation; nothing here knows Catan.
//
// Must be a pure function of its inputs: no I/O, no shared mutable state,
// deterministic — the same code runs on the server (source of truth) and on the
// client (to pre-validate before sending).
interface GameEngine<S, A, E> {
    fun initialState(players: List<PlayerId>): S

    // (state, who acted, what they did) -> result (new state + events, or a
    // rejection with the state unchanged).
    fun reduce(state: S, actor: PlayerId, action: A): GameResult<S, E>

    // Presence changes. A player leaving while it is their turn must hand the
    // turn to the next present player, otherwise the game stalls. A player
    // rejoining is simply marked present again — the turn order is unchanged.
    fun playerLeft(state: S, playerId: PlayerId): GameResult<S, E>
    fun playerJoined(state: S, playerId: PlayerId): GameResult<S, E>
}
