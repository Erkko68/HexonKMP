package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.PlayerSnapshot
import eric.bitria.hexon.game.data.ResourceId
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

/**
 * INTERNAL ENGINE CLASS
 * Holds the complete state of a player during the game.
 * This is NEVER sent directly to the client.
 */
data class GamePlayer(
    val id: String,
    val name: String,
    val color: String,
    val isHost: Boolean,

    // --- Dynamic Inventory ---
    // Maps ResourceId ("wood") to Quantity. Default 0.
    val resources: MutableMap<ResourceId, Int> = mutableMapOf(),
    val ports: MutableMap<String, Port> = mutableMapOf(),

    // Maps BuildingId ("settlement") to Count (e.g., 3 built so far).
    // Used to check limits (e.g., max 5 settlements).
    val buildingCounts: MutableMap<BuildingId, Int> = mutableMapOf(),

    // --- Development Cards ---
    // Stored as String IDs (e.g. "knight", "monopoly")
    val developmentCardsHand: MutableList<String> = mutableListOf(),
    val developmentCardsPlayed: MutableList<String> = mutableListOf(),

    // --- Status Flags ---
    var victoryPoints: Int = 0,
    var knightsPlayed: Int = 0,
    var hasLongestRoad: Boolean = false,
    var hasLargestArmy: Boolean = false,

    // --- Turn State ---
    // e.g. Did they play a dev card this turn already?
    var hasPlayedDevCardThisTurn: Boolean = false
) {

    /**
     * Helper to safely add resources.
     */
    fun addResources(changes: Map<ResourceId, Int>) {
        changes.forEach { (resId, amount) ->
            val current = resources[resId] ?: 0
            resources[resId] = current + amount
        }
    }

    /**
     * Helper to safely remove resources.
     * Returns false if insufficient funds.
     */
    fun tryDeductResources(cost: Map<ResourceId, Int>): Boolean {
        // 1. Check if they have enough
        val canAfford = cost.all { (resId, amount) ->
            (resources[resId] ?: 0) >= amount
        }
        if (!canAfford) return false

        // 2. Deduct
        cost.forEach { (resId, amount) ->
            resources[resId] = resources[resId]!! - amount
        }
        return true
    }

    /**
     * Counts total resource cards (for 7-roll logic or Robber).
     */
    fun totalResourceCount(): Int {
        return resources.values.sum()
    }

    /**
     * Checks if the player can afford the given cost.
     * @return true if they can, false otherwise.
     */
    fun canDeductResources(cost: Map<ResourceId, Int>): Boolean =
        cost.all { (res, amount) -> (resources[res] ?: 0) >= amount }

    /**
     * @param resource is not using typealias `ResourceID` to allow for null values representing generic ratio.
     * @return The best ratio discount for the given resource.
     */
    fun getPortDiscountRatio(resource: String?): Int =
        ports.values.find { it.resourceId == resource }?.ratio ?: 0

    /**
     * Converts this private state into the public-safe Snapshot
     * used for GameEvents.
     */
    fun toSnapshot(): PlayerSnapshot {
        return PlayerSnapshot(
            id = id,
            name = name,
            color = color,
            victoryPoints = victoryPoints, // Visible VPs (hidden VPs in dev cards usually kept secret)
            resourceCount = totalResourceCount(),
            devCardCount = developmentCardsHand.size,
            playedKnights = knightsPlayed,
            longestRoad = hasLongestRoad,
            largestArmy = hasLargestArmy
        )
    }
}