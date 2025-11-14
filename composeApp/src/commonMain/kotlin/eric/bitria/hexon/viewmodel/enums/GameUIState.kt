package eric.bitria.hexon.viewmodel.enums

enum class GameUIState {
    WAITING,
    ROLLING,
    PLAYING,
    TRADING,
    END_GAME
}

fun GameUIState.next(): GameUIState {
    val values = GameUIState.entries
    val nextIndex = (this.ordinal + 1) % values.size
    return values[nextIndex]
}