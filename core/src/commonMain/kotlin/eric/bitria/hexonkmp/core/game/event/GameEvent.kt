package eric.bitria.hexonkmp.core.game.event

import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.Road
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Resource
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

// The game moved to a new phase (e.g. Setup -> Play). Carries the new phase so
// clients can update which actions/affordances they offer.
@Serializable
@SerialName("PhaseChanged")
data class PhaseChanged(val phase: GamePhase) : GameEvent

// A settlement (or city, later) was placed.
@Serializable
@SerialName("BuildingPlaced")
data class BuildingPlaced(val building: Building) : GameEvent

// A road was placed.
@Serializable
@SerialName("RoadPlaced")
data class RoadPlaced(val road: Road) : GameEvent

// A settlement was upgraded to a city. [building] is the resulting city; clients
// replace the settlement at its vertex.
@Serializable
@SerialName("CityUpgraded")
data class CityUpgraded(val building: Building) : GameEvent

// The robber moved to [hex] (which now produces for nobody until moved again).
@Serializable
@SerialName("RobberMoved")
data class RobberMoved(val hex: Axial) : GameEvent

// The robber move stole one [resource] from [from], handed to [by].
@Serializable
@SerialName("ResourceStolen")
data class ResourceStolen(val from: PlayerId, val by: PlayerId, val resource: Resource) : GameEvent

// A player traded with the bank: their hand lost [given] and gained [received].
@Serializable
@SerialName("BankTraded")
data class BankTraded(
    val player: PlayerId,
    val given: ResourceCount,
    val received: ResourceCount,
) : GameEvent

// --- Player-to-player trades ---

// The current player proposed a trade offer (broadcast to all opponents).
@Serializable
@SerialName("TradeProposed")
data class TradeProposed(val offer: TradeOffer) : GameEvent

// An opponent accepted or declined a pending offer.
@Serializable
@SerialName("TradeResponded")
data class TradeResponded(val offerId: Int, val player: PlayerId, val accepted: Boolean) : GameEvent

// A trade was finalized between [proposer] and [partner]: the proposer lost
// [give] and gained [receive], the partner the reverse. Finalizing clears every
// pending offer, so clients drop all offers on this event.
@Serializable
@SerialName("TradeFinalized")
data class TradeFinalized(
    val offerId: Int,
    val proposer: PlayerId,
    val partner: PlayerId,
    val give: ResourceCount,
    val receive: ResourceCount,
) : GameEvent

// Pending offers were discarded (the proposer's turn ended). Clients clear them.
@Serializable
@SerialName("TradeOffersCleared")
data object TradeOffersCleared : GameEvent

// The proposer withdrew a single pending offer.
@Serializable
@SerialName("TradeCancelled")
data class TradeCancelled(val offerId: Int) : GameEvent
