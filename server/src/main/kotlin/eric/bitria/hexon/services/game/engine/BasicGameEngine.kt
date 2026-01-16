package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.dtos.matchmaking.GameMessage

class BasicGameEngine(
    private val sessionId: String
) : GameEngine {

    private lateinit var sender: GameMessageSender
    private val players = mutableListOf<String>()

    // Game State
    private var turnIndex = 0

    override suspend fun start(playerIds: List<String>, sender: GameMessageSender) {
        this.players.addAll(playerIds)
        this.sender = sender

        println("GAME ENGINE STARTED with ${players.size} players")

        // Broadcast "Game Started"
        sender.broadcast(GameMessage.GameUpdate(state = "GAME_START"))
    }

    override suspend fun onMessage(userId: String, message: GameMessage) {
        when (message) {
            is GameMessage.PlayerMove -> handleMove(userId, message)
            else -> {} // Ignore unknown types
        }
    }

    private suspend fun handleMove(userId: String, message: GameMessage.PlayerMove) {
        // 1. Validate Logic
        if (players[turnIndex] != userId) {
            sender.sendToPlayer(userId, GameMessage.Error("Not your turn"))
            return
        }

        // 2. Process Move
        // ... game logic ...
        turnIndex = (turnIndex + 1) % players.size

        // 3. Broadcast New State
        sender.broadcast(GameMessage.GameUpdate(
            state = "MOVE_MADE"
        ))
    }

    override suspend fun onPlayerLeave(userId: String) {
        sender.broadcast(GameMessage.Error("Player $userId disconnected"))
        // Handle pause or forfeit logic
    }

    override suspend fun onPlayerRejoin(userId: String) {
        // Send them the current board state
        sender.sendToPlayer(userId, GameMessage.GameUpdate("SYNC_STATE"))
    }
}