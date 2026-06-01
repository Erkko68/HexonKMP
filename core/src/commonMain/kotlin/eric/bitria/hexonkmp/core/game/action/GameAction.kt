package eric.bitria.hexonkmp.core.game.action

import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Client → server intents. Shared so the client can validate an action with the
// engine's reduce() *before* sending it, and so both sides speak one vocabulary.
@Serializable
sealed interface GameAction

// End the current player's turn (Play phase only).
@Serializable
@SerialName("EndTurn")
data object EndTurn : GameAction

// Place a settlement on a vertex. During setup this is the first half of a
// placement step; in play it will cost resources (added later).
@Serializable
@SerialName("PlaceSettlement")
data class PlaceSettlement(val vertex: Vertex) : GameAction

// Place a road on an edge. During setup this completes a placement step.
@Serializable
@SerialName("PlaceRoad")
data class PlaceRoad(val edge: Edge) : GameAction
