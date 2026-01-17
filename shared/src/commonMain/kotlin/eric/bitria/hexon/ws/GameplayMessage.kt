package eric.bitria.hexon.ws

import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayMessage : GameMessage() {
    // You will expand this later for DiceRoll, BuildRoad, Trade, etc.
}