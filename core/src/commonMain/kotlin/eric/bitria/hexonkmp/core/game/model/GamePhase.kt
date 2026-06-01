package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Which piece the current player must place next during setup.
@Serializable
enum class Placement { SETTLEMENT, ROAD }

// The phase of the game. Setup is the classic Catan snake draft; Play is normal
// turns with the automatic dice roll. The phase is authoritative state and is
// sent to clients (via PhaseChanged), so the UI can show the right affordances.
@Serializable
sealed interface GamePhase {

    // Snake-draft initial placement. `order` is the precomputed sequence of
    // player indices (e.g. for 3 players: 0,1,2,2,1,0); `index` points at whose
    // placement it is. Each step is a settlement followed by its road; `awaiting`
    // says which is expected, and `lastSettlement` is the settlement the next
    // road must connect to.
    @Serializable
    @SerialName("Setup")
    data class Setup(
        val order: List<Int>,
        val index: Int,
        val awaiting: Placement,
        val lastSettlement: Vertex? = null,
    ) : GamePhase {
        // True for the second pass of the snake — the settlement placed in this
        // round grants its adjacent tiles' resources as a starting hand.
        val isSecondRound: Boolean get() = index >= order.size / 2
    }

    @Serializable
    @SerialName("Play")
    data object Play : GamePhase
}
