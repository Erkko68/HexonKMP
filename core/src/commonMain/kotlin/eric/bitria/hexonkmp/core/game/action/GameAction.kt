package eric.bitria.hexonkmp.core.game.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Client → server intents. Shared so the client can validate an action with the
// engine's reduce() *before* sending it, and so both sides speak one vocabulary.
// Real Catan moves (BuildRoad, RollDice, Trade, …) join EndTurn here.
@Serializable
sealed interface GameAction

@Serializable
@SerialName("EndTurn")
data object EndTurn : GameAction
