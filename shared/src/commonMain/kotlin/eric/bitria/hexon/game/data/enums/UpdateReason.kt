package eric.bitria.hexon.game.data.enums

import kotlinx.serialization.Serializable

@Serializable
enum class UpdateReason {
    INITIAL,    // Initial resources
    PRODUCTION, // Dice roll
    BUILD,      // Spent resources
    TRADE,      // Swapped with player
    THEFT,      // Robber
    BANK,       // Maritime trade / 4:1
    DEV_CARD,   // Year of Plenty
    COST,       // Generic cost
    START       // Initial resources
}