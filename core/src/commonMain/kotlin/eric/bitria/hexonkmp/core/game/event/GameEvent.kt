package eric.bitria.hexonkmp.core.game.event

import eric.bitria.hexonkmp.core.game.model.PlayerId
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
