package eric.bitria.hexon.data.serializable.actions

import kotlinx.serialization.Serializable

@Serializable
sealed class HexonAction {
    abstract val type: String
}

