package eric.bitria.hexonkmp.core.game.config

import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlinx.serialization.Serializable

// Things you can build, used as keys for costs and per-player piece limits.
@Serializable
enum class Buildable { ROAD, SETTLEMENT, CITY, DEV_CARD }

// The tunable rules of a game mode — read by the engine instead of hardcoded
// constants. Swap these (or load them as data) to define variants.
@Serializable
data class RuleConfig(
    val minPlayers: Int = 2,
    val maxPlayers: Int = 4,
    val victoryPointsToWin: Int = 10,
    val buildCosts: Map<Buildable, Map<Resource, Int>>,
    val pieceLimits: Map<Buildable, Int>,
)
