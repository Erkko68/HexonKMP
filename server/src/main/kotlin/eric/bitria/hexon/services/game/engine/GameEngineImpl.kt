package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.enums.UpdateReason
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.lobby.LobbyPlayer

private enum class TurnPhase {
    SETUP,          // Initial settlement placement (Snake draft)
    WAITING_FOR_ROLL,
    MAIN_PHASE,     // Can build, trade, play cards
    ROBBER_RESOLUTION, // Waiting for a player to move the robber
    GAME_OVER
}

class GameEngineImpl(
    private val sessionId: String
) : GameEngine {

    private lateinit var sender: GameMessageSender
    private val lobbyPlayers = mutableListOf<LobbyPlayer>()

    // Game State
    private var turnIndex = 0
    private var currentPhase: TurnPhase = TurnPhase.SETUP

    override suspend fun start(lobbyPlayers: List<LobbyPlayer>, sender: GameMessageSender) {
        this.lobbyPlayers.addAll(lobbyPlayers)
        this.sender = sender

        println("GAME ENGINE STARTED with ${lobbyPlayers.size} players")

        // Broadcast "Game Started"
        //sender.broadcast(GameMessage.GameInfo(message = "GAME_START"))
    }

    override suspend fun onMessage(userId: String, message: GameMessage) {
        // We only care about Gameplay Intents here
        if (message !is GameplayIntent) return

        // Allow Players to Respond to Trade when its not their turn
        if (message is GameplayIntent.RespondToTrade){
            handleTradeResponse(userId, message)
        }

        // 1. Universal Validation (Is it this player's turn?)
        if (!validateTurn(userId, message)) return

        // 2. Route by Intent Type
        when (message) {
            is GameplayIntent.Build -> handleBuild(userId, message)
            is GameplayIntent.ProposeTrade -> handleTradeProposal(userId, message)
            is GameplayIntent.MoveRobber -> handleRobberMove(userId, message)
            is GameplayIntent.EndTurn -> handleEndTurn(userId)
            else -> sender.sendToPlayer(userId, GameplayEvent.GameError("Unknown action",
                GameErrorCode.GAME_ENDED))
        }
    }

    override suspend fun onPlayerLeave(userId: String) {
        //sender.broadcast(GameMessage.GameInfo("Player $userId disconnected"))
        // Handle pause or forfeit logic
    }

    override suspend fun onPlayerRejoin(userId: String) {
        // Send them the current board state
        //sender.sendToPlayer(userId, GameMessage.GameInfo("SYNC_STATE"))
    }

    private suspend fun rollDice() {
        // 1. Logic: Generate Random Number
        val roll1 = (1..6).random()
        val roll2 = (1..6).random()
        val total = roll1 + roll2

        // 2. Logic: Distribute Resources (Board interaction)
        currentPhase = if (total == 7) {
            TurnPhase.ROBBER_RESOLUTION
            // Notify clients to discard cards if > 7
        } else {
            // board.distributeResources(roll)
            TurnPhase.MAIN_PHASE
        }

        // 3. Broadcast Event
        sender.broadcast(GameplayEvent.DiceRolled(
            values = Pair(roll1, roll2),
            sum = total,
            sourcePlayerId = lobbyPlayers[turnIndex].id)
        )
        for (player in lobbyPlayers) {
            sender.sendToPlayer(
                player.id,
                GameplayEvent.ResourcesUpdated(
                    playerId = player.id,
                    changes = mapOf("wood" to 1),
                    reason = UpdateReason.PRODUCTION
                )
            )
        }
    }
}