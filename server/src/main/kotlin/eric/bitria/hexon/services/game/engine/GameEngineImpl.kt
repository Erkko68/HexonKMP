package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.dtos.matchmaking.GameMessage

class GameEngineImpl(
    private val sessionId: String
) : GameEngine {

    private lateinit var sender: GameMessageSender
    private val players = mutableListOf<Players>()

    // Game State
    private var turnIndex = 0

    override suspend fun start(players: List<Players>, sender: GameMessageSender) {
        this.players.addAll(players)
        this.sender = sender

        println("GAME ENGINE STARTED with ${players.size} players")

        // Broadcast "Game Started"
        sender.broadcast(GameMessage.GameInfo(message = "GAME_START"))
    }

    override suspend fun onMessage(userId: String, message: GameMessage) {
        when (message) {
            is GameMessage.GameInfo -> handleMove(userId, message)
            else -> {} // Ignore unknown types
        }
    }

    private suspend fun handleMove(userId: String, message: GameMessage) {
        // 3. Broadcast New State
        sender.broadcast(GameMessage.GameInfo("Broadcast Message by $userId",))
        sender.sendToPlayer(userId,GameMessage.GameInfo("Broadcast Message by $userId"))
    }

    override suspend fun onPlayerLeave(userId: String) {
        sender.broadcast(GameMessage.GameInfo("Player $userId disconnected"))
        // Handle pause or forfeit logic
    }

    override suspend fun onPlayerRejoin(userId: String) {
        // Send them the current board state
        sender.sendToPlayer(userId, GameMessage.GameInfo("SYNC_STATE"))
    }
}