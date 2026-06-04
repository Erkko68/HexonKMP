package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex

// The Catan engine: the generic transport contract bound to Catan's concrete
// types, plus the Catan-specific legal-move/affordability queries the CLIENT UI
// uses to offer only valid moves. The server only needs GameEngine; the client
// depends on this richer interface.
interface CatanEngine : GameEngine<GameState, GameAction, GameEvent> {
    // Pure legal-move queries — what `player` may place right now.
    fun legalSettlements(state: GameState, player: PlayerId): Set<Vertex>
    fun legalRoads(state: GameState, player: PlayerId): Set<Edge>

    // Vertices where `player` may upgrade a settlement to a city right now.
    fun legalCities(state: GameState, player: PlayerId): Set<Vertex>

    // Whether `player` can afford a buildable from their current hand. The UI
    // uses this to enable/disable build buttons during the Play phase.
    fun canAfford(state: GameState, player: PlayerId, buildable: Buildable): Boolean

    // The player's best bank-trade ratio for each resource: the base ratio
    // (RuleConfig.bankTradeRatio), lowered by any port whose vertex carries one of
    // the player's buildings. The UI uses this to highlight discounted resources
    // and to validate a give/receive draft before sending a BankTrade.
    fun bankRates(state: GameState, player: PlayerId): Map<Resource, Int>

    // Why a give/receive bank trade is illegal right now, or null if it's legal.
    // Single source of truth: reduce() uses this to validate a BankTrade, and the
    // UI uses it to enable/disable the "Trade with bank" button for the live draft.
    fun bankTradeRejection(
        state: GameState,
        player: PlayerId,
        give: ResourceCount,
        receive: ResourceCount,
    ): String?
}
