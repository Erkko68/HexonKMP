package eric.bitria.hexon.game.data.enums

enum class TurnPhase {
    SETUP,          // Initial settlement placement
    WAITING,        // Waiting for server/other players
    TRADE,
    MAIN_PHASE,     // Active turn (Build, Trade)
    ROBBER_RESOLUTION, // Discarding or moving robber
    GAME_OVER
}