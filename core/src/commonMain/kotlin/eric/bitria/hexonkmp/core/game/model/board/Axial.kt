package eric.bitria.hexonkmp.core.game.model.board

import kotlinx.serialization.Serializable

// Axial hex coordinate (q, r). Pure geometry — no Catan knowledge lives here.
// See https://www.redblobgames.com/grids/hexagons/ for the coordinate system.
@Serializable
data class Axial(val q: Int, val r: Int) : Comparable<Axial> {
    operator fun plus(other: Axial) = Axial(q + other.q, r + other.r)

    // Stable ordering so vertices/edges can be canonicalized by sorting.
    override fun compareTo(other: Axial): Int = compareValuesBy(this, other, { it.q }, { it.r })
}

// The six neighbor directions around a hex, in clockwise order. Corner k of a
// hex sits between directions k and k+1, which is how vertices are derived.
val AXIAL_DIRECTIONS: List<Axial> = listOf(
    Axial(1, 0), Axial(1, -1), Axial(0, -1),
    Axial(-1, 0), Axial(-1, 1), Axial(0, 1),
)

// All hex positions within `radius` rings of the center — a hexagonal board.
// radius 2 = 19 hexes (classic Catan), radius 3 = 37 (5–6 player expansion).
fun hexagonalLayout(radius: Int): List<Axial> = buildList {
    for (q in -radius..radius) {
        for (r in -radius..radius) {
            if (kotlin.math.abs(q + r) <= radius) add(Axial(q, r))
        }
    }
}
