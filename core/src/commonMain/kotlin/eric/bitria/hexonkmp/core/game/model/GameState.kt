package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.model.board.Board
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlinx.serialization.Serializable

// The authoritative game state. Pure data — the server owns the source of truth,
// the client renders a copy. It carries the ScenarioConfig it was created with,
// so the engine reads its rules from the state (data-driven) rather than from
// hardcoded constants. Catan domain (hands, buildings, dev cards, …) grows here.
@Serializable
data class GameState(
    val players: List<PlayerId>,        // seating order, fixed for the game
    val present: Set<PlayerId>,         // players currently connected
    val config: ScenarioConfig,
    val board: Board,
    val phase: GamePhase,
    val hands: Map<PlayerId, ResourceCount> = emptyMap(),
    val buildings: List<Building> = emptyList(),
    val roads: List<Road> = emptyList(),
    // Player-to-player trade offers live only during the proposer's turn; any
    // turn change clears them. `tradeCounter` is a monotonic id source (never
    // reset) so ids stay unique even after offers are cleared.
    val pendingTrades: List<TradeOffer> = emptyList(),
    val tradeCounter: Int = 0,
    val currentPlayerIndex: Int = 0,
    val turn: Int = 1,
    val lastRoll: Int? = null,          // most recent dice total (null before first roll)
    // --- Development cards (HIDDEN information — see redactedFor) ---
    // The remaining draw pile, in (secret) draw order. Cards come off the front.
    val devDeck: List<DevCard> = emptyList(),
    // Each player's playable dev cards (bought on a previous turn). Secret.
    val devCards: Map<PlayerId, List<DevCard>> = emptyMap(),
    // Cards bought THIS turn: not yet playable (Catan rule). They graduate into
    // [devCards] at the owner's next turn start. Secret.
    val boughtThisTurn: Map<PlayerId, List<DevCard>> = emptyMap(),
    // Public projections populated by [redactedFor] for client snapshots, and
    // otherwise empty/0 in the server's source-of-truth state (which reads the
    // real collections above). These are the only facts opponents may see about
    // hidden information: how many (unplayed) dev cards each player holds, how many
    // cards remain in the deck, and each player's total resource-card count.
    val devCardCounts: Map<PlayerId, Int> = emptyMap(),
    val devDeckSize: Int = 0,
    val resourceCounts: Map<PlayerId, Int> = emptyMap(),
    // Advances on each random draw so reduce() stays a pure function while dice
    // are effectively random. Seeded from the board seed at creation.
    val rngSeed: Long = 0L,
) : Redactable<GameState> {
    val currentPlayer: PlayerId get() = players[currentPlayerIndex]

    fun handOf(player: PlayerId): ResourceCount = hands[player] ?: ResourceCount()

    fun buildingAt(vertex: Vertex): Building? = buildings.firstOrNull { it.vertex == vertex }

    fun roadAt(edge: Edge): Road? = roads.firstOrNull { it.edge == edge }

    // Total unplayed dev cards a player holds (playable + bought-this-turn). This
    // count is public in Catan even though the card TYPES are not.
    fun devCardCountOf(player: PlayerId): Int =
        (devCards[player]?.size ?: 0) + (boughtThisTurn[player]?.size ?: 0)

    // The per-recipient projection sent to a client: this is THE transport seam for
    // hidden information. It strips every secret another player must not see — the
    // deck's order/contents, all opponents' dev-card types, and all opponents'
    // resource cards — while preserving the public facts (each player's dev-card
    // count, resource-card count, and the deck size). The viewer keeps full
    // visibility of their own cards.
    override fun redactedFor(viewer: PlayerId): GameState = copy(
        devDeck = emptyList(),
        devCards = devCards.filterKeys { it == viewer },
        boughtThisTurn = boughtThisTurn.filterKeys { it == viewer },
        hands = hands.filterKeys { it == viewer },
        devCardCounts = players.associateWith { devCardCountOf(it) },
        devDeckSize = devDeck.size,
        resourceCounts = players.associateWith { handOf(it).total },
    )
}
