package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.model.board.Board
import kotlinx.serialization.Serializable

// The authoritative game state. Pure data — the server owns the source of truth,
// the client renders a copy. It carries the ScenarioConfig it was created with,
// so the engine reads its rules from the state (data-driven) rather than from
// hardcoded constants. Catan domain (hands, buildings, dev cards, …) grows here.
@Serializable
data class GameState(
    val players: List<PlayerId>,        // seating order, fixed for the game
    val present: Set<PlayerId>,         // players currently connected
    val config: ScenarioConfig,
    val board: Board,
    val currentPlayerIndex: Int = 0,
    val turn: Int = 1,
) {
    val currentPlayer: PlayerId get() = players[currentPlayerIndex]
}
