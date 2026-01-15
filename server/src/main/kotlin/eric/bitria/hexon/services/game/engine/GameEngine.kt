package eric.bitria.hexon.services.game.engine

interface GameEngine {

    val gameId: String

    /** Initialize the game with players */
    fun initialize(players: List<String>)

    /** Apply a player move. Returns true if valid */
    fun applyMove(playerId: String, move: Any): Boolean

    /** Get current game state (for sending to clients) */
    fun getState(): Any

    /** Check if game has finished */
    fun isFinished(): Boolean
}