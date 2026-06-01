package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.model.board.Board
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
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
    val phase: GamePhase,
    val hands: Map<PlayerId, ResourceCount> = emptyMap(),
    val buildings: List<Building> = emptyList(),
    val roads: List<Road> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val turn: Int = 1,
    val lastRoll: Int? = null,          // most recent dice total (null before first roll)
    // Advances on each random draw so reduce() stays a pure function while dice
    // are effectively random. Seeded from the board seed at creation.
    val rngSeed: Long = 0L,
) {
    val currentPlayer: PlayerId get() = players[currentPlayerIndex]

    fun handOf(player: PlayerId): ResourceCount = hands[player] ?: ResourceCount()

    fun buildingAt(vertex: Vertex): Building? = buildings.firstOrNull { it.vertex == vertex }

    fun roadAt(edge: Edge): Road? = roads.firstOrNull { it.edge == edge }
}
