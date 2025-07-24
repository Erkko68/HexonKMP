package eric.bitria.hexon.data.serializable.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ROLL_DICE")
data class RollDiceAction(
    override val type: String = "ROLL_DICE",
    val playerId: String
) : HexonAction()