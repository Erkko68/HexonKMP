package eric.bitria.hexonkmp.core.game.board

import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.model.board.Board
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import eric.bitria.hexonkmp.core.game.model.board.Tile
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
        return Board(tiles = tiles, robber = robber)
    }
}
