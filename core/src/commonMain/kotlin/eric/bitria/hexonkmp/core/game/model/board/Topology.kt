package eric.bitria.hexonkmp.core.game.model.board

import kotlinx.serialization.Serializable

// A vertex (intersection) where settlements/cities go. Identified canonically by
// the set of hex positions meeting at that corner (up to 3) — so the same corner
// shared by neighboring hexes is always the same Vertex. Pure topology.
@Serializable
data class Vertex(val hexes: List<Axial>) {
    companion object {
        fun of(a: Axial, b: Axial, c: Axial): Vertex =
            Vertex(listOf(a, b, c).sorted())
    }
}

// An edge (border between two hex positions) where roads go. Identified by the
// two hex positions it separates. Border edges still reference a neighbor
// position that may have no tile — that's intentional and correct (coastal roads).
@Serializable
data class Edge(val hexes: List<Axial>) {
    companion object {
        fun of(a: Axial, b: Axial): Edge =
            Edge(listOf(a, b).sorted())
    }
}

// Derives the canonical vertex at corner `k` (0..5) of `hex`: the corner sits
// between neighbor directions k and k+1.
fun cornerVertex(hex: Axial, k: Int): Vertex =
    Vertex.of(hex, hex + AXIAL_DIRECTIONS[k], hex + AXIAL_DIRECTIONS[(k + 1) % 6])

// Derives the edge in direction `k` (0..5) from `hex`.
fun directionEdge(hex: Axial, k: Int): Edge =
    Edge.of(hex, hex + AXIAL_DIRECTIONS[k])
