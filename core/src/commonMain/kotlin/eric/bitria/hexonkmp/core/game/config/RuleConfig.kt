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
    // Largest Army: the fewest knights that earns the award, and the victory points
    // it's worth (held by whoever has played the most knights, once past the min).
    val largestArmyMin: Int = 3,
    val largestArmyVp: Int = 2,
    // Longest Road: the minimum road-chain length to claim the award, and its VP value.
    // Incumbent keeps the title on ties — a challenger must exceed, not match.
    val longestRoadMin: Int = 5,
    val longestRoadVp: Int = 2,
    // How many identical resources the bank takes for one of any other resource
    // (classic Catan = 4:1; ports lower this, added later).
    val bankTradeRatio: Int = 4,
    // On a 7, players holding MORE than this many cards discard half (floor).
    val robberDiscardThreshold: Int = 7,
    // Matchmaking: once a lobby reaches [minPlayers] but isn't yet full, how long
    // to keep waiting for more players before starting the game automatically. The
    // game starts immediately if [maxPlayers] is reached first.
    val autoStartDelaySeconds: Int = 30,
    // How long each player has on their turn before the server auto-resolves it
    // (auto-ends a Play turn; auto-resolves a pending robber/steal/road decision).
    // null means no timer — a turn ends only when the player acts (manual skip).
    val turnTimerSeconds: Int? = 45,
) {
    // The cost of a buildable as a ResourceCount (empty if free/undefined).
    fun cost(buildable: Buildable): ResourceCount =
        ResourceCount(buildCosts[buildable] ?: emptyMap())
}
