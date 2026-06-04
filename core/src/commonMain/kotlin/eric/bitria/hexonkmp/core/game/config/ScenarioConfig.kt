package eric.bitria.hexonkmp.core.game.config

import eric.bitria.hexonkmp.core.game.model.Port
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import kotlinx.serialization.Serializable

// A complete description of a game mode, as pure data. The board layout, the
// tile/token distribution, and the rules all live here — so adding a variant
// means authoring a new ScenarioConfig, never editing the engine.
//
// `terrainBag` is the multiset of terrains to place (one per hex in `hexLayout`),
// and `numberTokens` is the bag of chits assigned to non-desert tiles. The
// BoardGenerator shuffles and places them deterministically from a seed.
@Serializable
data class ScenarioConfig(
    val name: String,
    val hexLayout: List<Axial>,
    val terrainBag: List<Terrain>,
    val numberTokens: List<Int>,
    val rules: RuleConfig,
    // Harbors: per-vertex trade discounts. A building on one of these vertices
    // lowers the owner's bank-trade ratio (see Port). Empty = no ports.
    val ports: List<Port> = emptyList(),
) {
    init {
        require(terrainBag.size == hexLayout.size) {
            "terrainBag (${terrainBag.size}) must match hexLayout (${hexLayout.size})"
        }
        val nonDesert = terrainBag.count { it != Terrain.DESERT }
        require(numberTokens.size == nonDesert) {
            "numberTokens (${numberTokens.size}) must match non-desert tiles ($nonDesert)"
        }
    }
}
