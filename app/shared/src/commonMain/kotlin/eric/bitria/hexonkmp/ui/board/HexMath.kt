package eric.bitria.hexonkmp.ui.board

import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Pure axial-hex -> world-space (XZ plane) conversion for the 3D board. No
// Filament deps so it can be unit-tested. Flat-top hexagons: neighbours sit
// along the X axis and the two diagonals.
//
// World layout: the board lies on the XZ plane (Y up). `hexSize` is the distance
// from a hex centre to any of its corners (the circumradius).
object HexMath {

    private const val SQRT3 = 1.7320508f // sqrt(3)

    // A flat-top hex's centre in world space. Standard axial->pixel for flat-top:
    //   x = size * 3/2 * q
    //   z = size * sqrt3 * (r + q/2)
    fun center(hex: Axial, hexSize: Float): WorldPos {
        val x = hexSize * 1.5f * hex.q
        val z = hexSize * SQRT3 * (hex.r + hex.q / 2f)
        return WorldPos(x, z)
    }

    // World position of a flat-top hex corner k (0..5). Corner k of cornerVertex()
    // sits between neighbour directions k and k+1; geometrically that's the corner
    // at angle 60°*k for a flat-top hex.
    fun corner(hex: Axial, k: Int, hexSize: Float): WorldPos {
        val c = center(hex, hexSize)
        val angle = (PI.toFloat() / 3f) * k // 60° steps, 0° = +X (flat-top)
        return WorldPos(c.x + hexSize * cos(angle), c.z + hexSize * sin(angle))
    }

    // The world position of a settlement vertex: the centroid of the (1..3) hex
    // centres that meet there. Works for coastal corners with fewer than 3 hexes.
    fun vertexCenter(vertex: Vertex, hexSize: Float): WorldPos {
        val centers = vertex.hexes.map { center(it, hexSize) }
        return WorldPos(centers.map { it.x }.average().toFloat(), centers.map { it.z }.average().toFloat())
    }

    // Midpoint of an edge: average of its two hex centres' shared border, i.e. the
    // midpoint between the two hex centres projected to their shared side. Using
    // the midpoint of the two endpoint vertices gives the true edge centre.
    fun edgeCenter(edge: Edge, hexSize: Float): WorldPos {
        val centers = edge.hexes.map { center(it, hexSize) }
        return WorldPos(centers.map { it.x }.average().toFloat(), centers.map { it.z }.average().toFloat())
    }

    // Rotation (radians, about the Y axis) so a road quad aligns along an edge.
    // The edge runs perpendicular to the line joining its two hex centres.
    fun edgeAngleY(edge: Edge, hexSize: Float): Float {
        val (a, b) = edge.hexes
        val ca = center(a, hexSize)
        val cb = center(b, hexSize)
        // Direction from a to b; the road sits across it, so rotate by 90°.
        val dx = cb.x - ca.x
        val dz = cb.z - ca.z
        return atan2(dz, dx)
    }
}

// A point on the board plane. `x`/`z` are world coordinates; Y is always the
// board surface (0) unless a renderable lifts it.
data class WorldPos(val x: Float, val z: Float)
