package eric.bitria.hexon.game.data.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GameErrorCode {
    NOT_YOUR_TURN,
    INSUFFICIENT_RESOURCES,
    INVALID_PLACEMENT,
    INVALID_TRADE,
    UNKNOWN_BUILDING,
    GAME_ENDED
}