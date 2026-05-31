package eric.bitria.hexonkmp.core.game.model

import kotlinx.serialization.Serializable

// The authoritative game state. Pure data — the server owns the source of truth,
// the client renders a copy. Catan domain (board, hands, buildings, …) will grow
// here as fields are added; nothing in this package knows about transport.
@Serializable
data class GameState(
    val players: List<PlayerId>,
    val currentPlayerIndex: Int = 0,
    val turn: Int = 1,
) {
    val currentPlayer: PlayerId get() = players[currentPlayerIndex]
}
