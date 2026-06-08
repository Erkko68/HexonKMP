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

    // After a 7: players holding too many cards must discard half before the
    // robber moves. [pending] maps each such player to how many they must discard;
    // the phase ends once no *present* player still owes. Absent players keep their
    // entry but don't block progress.
    @Serializable
    @SerialName("Discard")
    data class Discard(val pending: Map<PlayerId, Int>) : GamePhase

    // The current player rolled a 7 and must move the robber before continuing.
    // Building/trading/ending the turn all gate on Play, so they're blocked here.
    @Serializable
    @SerialName("Robber")
    data object Robber : GamePhase

    // Playing a Road Building dev card: the current player places [roadsLeft] free
    // roads before returning to Play. Decrements on each PlaceRoad; reaches 0 ->
    // engine switches back to Play automatically.
    @Serializable
    @SerialName("RoadBuilding")
    data class RoadBuilding(val roadsLeft: Int) : GamePhase

    // After the robber lands on a tile with 2+ eligible opponents: the roller must
    // pick who to steal from. [victims] are the eligible player ids (each has a
    // building on the robber tile and at least one resource card).
    @Serializable
    @SerialName("ChooseStealTarget")
    data class ChooseStealTarget(val victims: List<PlayerId>) : GamePhase

    // The game is over — [winner] reached the victory-point goal. Terminal: no
    // further actions are accepted.
    @Serializable
    @SerialName("Finished")
    data class Finished(val winner: PlayerId) : GamePhase
}
