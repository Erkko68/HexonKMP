package eric.bitria.hexonkmp.core.game.model.board

import eric.bitria.hexonkmp.core.game.model.Port
import kotlinx.serialization.Serializable

// One hex tile: its position, terrain, and number token (null on the desert).
// A tile rolls its resource when the dice match `token`.
@Serializable
data class Tile(
    val hex: Axial,
    val terrain: Terrain,
    val token: Int? = null,
)

// The generated board: the concrete tiles, the robber's position, and the placed
// harbors. This is authoritative state and is serialized to clients. Vertices and
// edges are not stored — they're pure functions of the tiles, derived on demand.
@Serializable
data class Board(
    val tiles: List<Tile>,
    val robber: Axial?,
    val ports: List<Port> = emptyList(),
) {
    private val tileByHex: Map<Axial, Tile> get() = tiles.associateBy { it.hex }

    fun tileAt(hex: Axial): Tile? = tileByHex[hex]

    // All buildable vertices: every corner touching at least one tile.
    fun vertices(): Set<Vertex> = buildSet {
        for (tile in tiles) for (k in 0..5) add(cornerVertex(tile.hex, k))
    }

    // All buildable edges: every side touching at least one tile.
    fun edges(): Set<Edge> = buildSet {
        for (tile in tiles) for (k in 0..5) add(directionEdge(tile.hex, k))
    }
}
