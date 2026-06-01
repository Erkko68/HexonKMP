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

// --- Adjacency (pure topology, used by placement rules) ---

// The (up to two) vertices at the ends of an edge: the corners that contain both
// of the edge's hexes.
fun Edge.endpoints(): List<Vertex> {
    val (a, b) = hexes
    return (0..5).map { cornerVertex(a, it) }.filter { b in it.hexes }.distinct()
}

// The three edges meeting at a vertex: one between each pair of its hexes.
fun Vertex.incidentEdges(): List<Edge> {
    val (a, b, c) = hexes
    return listOf(Edge.of(a, b), Edge.of(b, c), Edge.of(a, c))
}

// Two vertices are neighbors if they share an edge — i.e. share exactly two
// hexes. This is the basis of the settlement distance rule.
fun Vertex.isAdjacentTo(other: Vertex): Boolean =
    this != other && hexes.intersect(other.hexes.toSet()).size == 2

// Vertices reachable in one edge-step from this one.
fun Vertex.adjacentVertices(): List<Vertex> =
    incidentEdges().flatMap { it.endpoints() }.filter { it != this }.distinct()

// An edge touches a vertex if that vertex is one of its endpoints.
fun Edge.touches(vertex: Vertex): Boolean = vertex in endpoints()

// Two edges are connected if they share an endpoint vertex (roads link up).
fun Edge.isConnectedTo(other: Edge): Boolean =
    this != other && endpoints().any { it in other.endpoints() }
