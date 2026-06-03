package eric.bitria.hexonkmp.core.game.config

import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.ResourceCount
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
    // The development-card deck composition: how many of each card type to shuffle
    // into the draw pile. Data, so a variant just lists different counts.
    val devCardDeck: Map<DevCard, Int> = emptyMap(),
    // How many identical resources the bank takes for one of any other resource
    // (classic Catan = 4:1; ports lower this, added later).
    val bankTradeRatio: Int = 4,
    // On a 7, players holding MORE than this many cards discard half (floor).
    val robberDiscardThreshold: Int = 7,
    // Matchmaking: once a lobby reaches [minPlayers] but isn't yet full, how long
    // to keep waiting for more players before starting the game automatically. The
    // game starts immediately if [maxPlayers] is reached first.
    val autoStartDelaySeconds: Int = 30,
) {
    // The cost of a buildable as a ResourceCount (empty if free/undefined).
    fun cost(buildable: Buildable): ResourceCount =
        ResourceCount(buildCosts[buildable] ?: emptyMap())
}
