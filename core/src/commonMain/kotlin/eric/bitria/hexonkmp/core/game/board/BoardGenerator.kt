package eric.bitria.hexonkmp.core.game.board

import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.model.Port
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Board
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import eric.bitria.hexonkmp.core.game.model.board.Tile
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.game.model.board.directionEdge
import eric.bitria.hexonkmp.core.game.model.board.endpoints
import kotlin.random.Random

// Turns a ScenarioConfig into a concrete Board. Pure and deterministic given a
// seed: the same seed always produces the same board, which makes it testable
// and reproducible (and lets a server share the seed if it ever needs to).
object BoardGenerator {

    fun generate(config: ScenarioConfig, seed: Long): Board {
        val random = Random(seed)
        val terrains = config.terrainBag.shuffled(random)
        val tokens = config.numberTokens.shuffled(random).iterator()

        val tiles = config.hexLayout.zip(terrains) { hex, terrain ->
            // Desert gets no token; every other tile draws the next chit.
            val token = if (terrain == Terrain.DESERT) null else tokens.next()
            Tile(hex = hex, terrain = terrain, token = token)
        }

        // The robber starts on the desert (classic rule); null if no desert.
        val robber = tiles.firstOrNull { it.terrain == Terrain.DESERT }?.hex
        val ports = placePorts(config, random)
        return Board(tiles = tiles, robber = robber, ports = ports)
    }

    // Shuffles the scenario's portBag onto coastline edges. A coastline edge has
    // exactly one of its two hexes on the board (the other is open sea). Harbors are
    // chosen greedily so no two share a vertex (each settlement spot reaches at most
    // one harbor), then the bag's kinds are assigned in shuffled order. Both choice
    // and assignment are driven by [random], so harbors vary per game with the seed.
    private fun placePorts(config: ScenarioConfig, random: Random): List<Port> {
        if (config.portBag.isEmpty()) return emptyList()
        val board = config.hexLayout.toSet()
        val coastEdges: List<Edge> = buildSet {
            for (hex in config.hexLayout) for (k in 0..5) {
                val edge = directionEdge(hex, k)
                val (a, b) = edge.hexes
                if ((a in board) != (b in board)) add(edge)
            }
        }.shuffled(random)

        val usedVertices = mutableSetOf<Vertex>()
        val chosen = mutableListOf<Edge>()
        for (edge in coastEdges) {
            if (chosen.size == config.portBag.size) break
            val ends = edge.endpoints()
            if (ends.any { it in usedVertices }) continue
            chosen += edge
            usedVertices += ends
        }

        val kinds = config.portBag.shuffled(random)
        return chosen.mapIndexed { i, edge -> Port(edge, kinds[i].resource, kinds[i].ratio) }
    }
}
