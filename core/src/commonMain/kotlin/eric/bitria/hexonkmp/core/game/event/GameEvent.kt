package eric.bitria.hexonkmp.core.game.event

import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Domain deltas produced by the engine when an action is applied. Pure: they
// describe *what changed*, with no knowledge of how they reach a client (the
// server wraps them in a transport envelope before broadcasting).
@Serializable
sealed interface GameEvent

@Serializable
@SerialName("TurnChanged")
data class TurnChanged(val currentPlayer: PlayerId, val turn: Int) : GameEvent

// The automatic roll at the start of a player's turn. Carries both dice so the
// UI can show them; `total` is their sum (the producing number).
@Serializable
@SerialName("DiceRolled")
data class DiceRolled(val die1: Int, val die2: Int, val total: Int) : GameEvent

// Resources handed out after a roll, per player. Empty map if the roll produced
// nothing (e.g. a 7, or no buildings on matching tiles yet).
@Serializable
@SerialName("ResourcesProduced")
data class ResourcesProduced(val gains: Map<PlayerId, ResourceCount>) : GameEvent
