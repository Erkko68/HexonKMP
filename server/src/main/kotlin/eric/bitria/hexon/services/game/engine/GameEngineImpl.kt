package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.Board
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.BuildingDef
import eric.bitria.hexon.game.data.GameConfig
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlacementType
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceDef
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.enums.UpdateReason
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.lobby.LobbyPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.forEach

private enum class TurnPhase {
    SETUP,          // Initial settlement placement (Snake draft)
    WAITING_FOR_ROLL,
    MAIN_PHASE,     // Can build, trade, play cards
    ROBBER_RESOLUTION, // Waiting for a player to move the robber
    GAME_OVER
}

class GameEngineImpl(
    private val sessionId: String,
    private val gameConfig: GameConfig = GameConfigLoader.default()
) : GameEngine {

    // Infrastructure
    private lateinit var sender: GameMessageSender

    // Internal State
    private val players = ConcurrentHashMap<String, GamePlayer>()
    private val board = Board()
    val buildings: Map<String, BuildingDef> =
        gameConfig.buildings.associateBy { it.id }
    val resources: Map<String, ResourceDef> =
        gameConfig.resources.associateBy { it.id }

    // Game Loop State
    private var turnIndex = 0
    private var currentTurnPlayerId: String = ""
    private var currentPhase: TurnPhase = TurnPhase.SETUP

    override suspend fun start(lobbyPlayers: List<LobbyPlayer>, sender: GameMessageSender) {
        this.sender = sender

        // 1. Initialize Internal State
        lobbyPlayers.forEach {
            players[it.id] = GamePlayer(it.id, it.name, it.color, it.isHost)
        }

        // 2. Map Generation (Procedural Logic)
        initializeBoard()

        // 3. Send Configuration & Initial State to Clients
        sender.broadcast(GameplayEvent.GameConfigLoaded(gameConfig))

        // 4. Kickoff the Loop
        startFirstTurn()
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
                GameErrorCode.UNKNOWN_BUILDING))
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
        var production = mapOf<String, MutableMap<ResourceId, Int>>()

        // 2. Logic: Distribute Resources (Board interaction)
        currentPhase = if (total == 7) {
            TurnPhase.ROBBER_RESOLUTION
            // Notify clients to discard cards if > 7
        } else {
            production = board.getProductionForRoll(total)
            TurnPhase.MAIN_PHASE
        }

        // 3. Broadcast Event
        sender.broadcast(
            GameplayEvent.DiceRolled(
                values = Pair(roll1, roll2),
                sum = total,
                sourcePlayerId = currentTurnPlayerId
            )
        )

        production.forEach { (playerId, resources) ->
            // Store Player Resources
            players[playerId]?.addResources(resources)
            // Notify Resources Update
            sender.sendToPlayer(
                playerId,
                GameplayEvent.ResourcesUpdated(
                    playerId = playerId,
                    changes = resources,
                    reason = UpdateReason.PRODUCTION
                )
            )
        }
    }

    private suspend fun handleBuild(userId: PlayerId, intent: GameplayIntent.Build) {
        val buildingDef = buildings[intent.buildingId]
            ?: return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Unknown building", GameErrorCode.UNKNOWN_BUILDING)
            )

        if (!players[userId]?.tryDeductResources(buildingDef.cost)!!){
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES)
            )
        }

        // Convert HexCoords to String ID
        val locationId = if(buildingDef.type == PlacementType.EDGE){
            HexCoord.getEdgeId(intent.hexA, intent.hexB)
        } else {
            if (intent.hexC == null) return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Invalid placement, expected building type Vertex but coord C was null", GameErrorCode.INVALID_PLACEMENT)
            )
            HexCoord.getVertexId(intent.hexA, intent.hexB, intent.hexC!!)
        }

        board.placeBuilding(
            buildingDef.id,
            userId,
            locationId,
            buildingDef.type
        )

        sender.broadcast(
            GameplayEvent.ObjectBuilt(
                playerId = userId,
                buildingId = buildingDef.id,
                hexA = intent.hexA,
                hexB = intent.hexB,
                hexC = intent.hexC
            )
        )
    }

    private suspend fun handleRobberMove(userId: PlayerId, intent: GameplayIntent.MoveRobber) {
        val playerIds = board.moveRobber(intent.hexA)

        // Broadcast robber update
        sender.broadcast(
            GameplayEvent.RobberUpdated(
                location = intent.hexA
            )
        )

        if (playerIds.isEmpty()) return

        val victimId = playerIds.random()
        val victimResources = players[victimId]?.resources ?: return

        // Pick a random resource type the victim has
        val stolenResource = victimResources
            .filter { it.value > 0 }
            .keys
            .randomOrNull()
            ?.let { mapOf(it to 1) } ?: return

        // Remove from victim
        players[victimId]?.addResources(stolenResource)
        sender.sendToPlayer(
            victimId,
            GameplayEvent.ResourcesUpdated(
                playerId = victimId,
                changes = stolenResource,
                reason = UpdateReason.THEFT
            )
        )

        // Give to robber player
        players[userId]?.addResources(stolenResource)
        sender.sendToPlayer(
            userId,
            GameplayEvent.ResourcesUpdated(
                playerId = userId,
                changes = stolenResource,
                reason = UpdateReason.THEFT
            )
        )
    }

    private suspend fun handleTradeProposal(userId: PlayerId, intent: GameplayIntent.ProposeTrade) {
        val player = players[userId] ?: return
        val tradeGive = intent.offer.give

        // Check if player has enough resources to give
        if (!player.canDeductResources(tradeGive)) return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES)
        )

        // Send trade proposal to the receiver
        sender.sendToPlayer(
            intent.receiverPlayerId,
            GameplayEvent.TradeProposed(
                tradeId = intent.offer.id,
                proposerId = userId,
                offer = intent.offer
            )
        )
    }
}