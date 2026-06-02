package eric.bitria.hexonkmp.core.game.model

import kotlinx.serialization.Serializable

// A pending player-to-player trade. The proposer (current player) offers to give
// [give] in exchange for [receive], broadcast to every opponent; each may accept
// or decline (recorded in [responses]). The proposer later finalizes with one
// accepter. Pure data — the engine validates and applies it. See trading-design.
@Serializable
data class TradeOffer(
    val id: Int,
    val proposer: PlayerId,
    val give: ResourceCount,      // what the proposer gives up
    val receive: ResourceCount,   // what the proposer wants in return
    val responses: Map<PlayerId, Boolean> = emptyMap(), // responder -> accepted?
) {
    // Players who have accepted (the proposer may finalize with any of these).
    val accepters: Set<PlayerId> get() = responses.filterValues { it }.keys
}
