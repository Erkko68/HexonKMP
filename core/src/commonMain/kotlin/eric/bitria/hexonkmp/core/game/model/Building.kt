package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlinx.serialization.Serializable

// A settlement or city placed on a board vertex, owned by a player. The kind
// determines how many resources the vertex collects when an adjacent tile
// produces (settlement = 1, city = 2).
@Serializable
data class Building(
    val owner: PlayerId,
    val vertex: Vertex,
    val kind: Kind,
) {
    @Serializable
    enum class Kind(val yield: Int) { SETTLEMENT(1), CITY(2) }
}
