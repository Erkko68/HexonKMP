package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.Board
import eric.bitria.hexon.game.GameConfigLoader
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.BuildingDef
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.PlacementType
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceDef
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.enums.UpdateReason
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.lobby.LobbyPlayer
import kotlin.collections.forEach

private enum class TurnPhase {
    SETUP,          // Initial settlement placement (Snake draft)
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
    private val players = mutableMapOf<String, GamePlayer>()
    private val board = Board(
        availableResources = gameConfig.resources,
        availableBuildings = gameConfig.buildings
    )
    val buildings: Map<BuildingId, BuildingDef> =
        gameConfig.buildings.associateBy { it.id }
    val resources: Map<ResourceId, ResourceDef> =
        gameConfig.resources.associateBy { it.id }
    val trades = mutableMapOf<PlayerId, TradeOffer>()

    // Game Loop State
    private val playerQueue = mutableListOf<String>()
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
        board.initializeBoard()

        // 3. Send Configuration & Initial State to Clients
        sender.broadcast(GameplayEvent.GameConfigLoaded(gameConfig))

        // 4. Kickoff the Loop
        startFirstTurn()
    }

    override suspend fun onMessage(userId: String, message: GameMessage) {
        // We only care about Gameplay Intents here
        if (message !is GameplayIntent) return sender.sendToPlayer(userId, GameplayEvent.GameError("Unknown intent action",
            GameErrorCode.UNKNOW_ACTION))

        // Allow Players to Respond or Offer Trades with each other
        when(message) {
            is GameplayIntent.ProposeTrade -> return handleTradeProposal(userId, message)
            is GameplayIntent.RespondToTrade -> return handleTradeResponse(userId, message)
            is GameplayIntent.ConfirmTrade -> return handleTradeConfirmation(userId, message)
            else -> {}
        }

        // 1. Is it this player's turn?
        if (userId != currentTurnPlayerId) return sender.sendToPlayer(userId, GameplayEvent.GameError("Not your turn",
            GameErrorCode.NOT_YOUR_TURN))

        // 2. Route by Intent Type
        when (message) {
            is GameplayIntent.Build -> handleBuild(userId, message)
            is GameplayIntent.MoveRobber -> handleRobberMove(userId, message)
            is GameplayIntent.ExchangeWithBank -> handleExchangeWithBank(userId, message)
            is GameplayIntent.EndTurn -> handleEndTurn(userId)
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

    private suspend fun startFirstTurn(){
        // Populate playerQueue
        players.forEach { (playerId, _) ->
            playerQueue.add(playerId)
        }

        // Get first player
        playerQueue.shuffle()
        currentTurnPlayerId = playerQueue.elementAt(turnIndex)

        // Notify first player
        sender.broadcast(GameplayEvent.TurnChanged(newPlayerId = currentTurnPlayerId))
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
                sum = total
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

    private suspend fun handleEndTurn(playerId: PlayerId){
        // 1. Calculate Next Player
        turnIndex = (turnIndex + 1) % playerQueue.size
        currentTurnPlayerId = playerQueue.elementAt(turnIndex)

        // 3. Notify Everyone
        sender.broadcast(GameplayEvent.TurnChanged(newPlayerId = currentTurnPlayerId))

        // 4. Roll dice
        rollDice()
    }

    private suspend fun handleBuild(userId: PlayerId, intent: GameplayIntent.Build) {
        val player = players[userId] ?: return
        val def = buildings[intent.buildingId]
            ?: return sender.sendToPlayer(userId, GameplayEvent.GameError("Unknown building", GameErrorCode.UNKNOWN_BUILDING))

        if (!player.tryDeductResources(def.cost)) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES))
        }

        val success = when (def.type) {
            PlacementType.VERTEX -> {
                val h3 = intent.hexC ?: return sender.sendToPlayer(
                    userId, GameplayEvent.GameError("Vertex building requires 3 coordinates", GameErrorCode.INVALID_PLACEMENT)
                )
                // Validate Rules (skip if Setup phase)
                if (currentPhase != TurnPhase.SETUP && !board.canPlaceVertexBuilding(userId, intent.hexA, intent.hexB, h3, def.id)) {
                    return sender.sendToPlayer(userId, GameplayEvent.GameError("Invalid placement rule", GameErrorCode.INVALID_PLACEMENT))
                }
                board.placeVertexBuilding(def.id, userId, intent.hexA, intent.hexB, h3)
            }
            PlacementType.EDGE -> {
                // Validate Rules
                if (!board.canPlaceEdgeBuilding(userId, intent.hexA, intent.hexB, def.id)) {
                    return sender.sendToPlayer(userId, GameplayEvent.GameError("Invalid placement rule", GameErrorCode.INVALID_PLACEMENT))
                }
                board.placeEdgeBuilding(def.id, userId, intent.hexA, intent.hexB)
            }
        }

        if (!success) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Placement failed (Internal Logic)", GameErrorCode.INVALID_PLACEMENT))
        }

        sender.broadcast(
            GameplayEvent.ObjectBuilt(
                playerId = userId,
                buildingId = def.id,
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
        players[victimId]?.tryDeductResources(stolenResource)
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

        // Store new trade for the player
        trades[userId] = intent.offer

        // Send trade proposal to the receiver
        sender.broadcast(
            GameplayEvent.TradeProposed(
                offer = intent.offer,
                senderId = userId // Need to notify from who this trade is coming from
            )
        )
    }

    private suspend fun handleTradeResponse(userId: PlayerId, intent: GameplayIntent.RespondToTrade) {
        val player = players[userId] ?: return

        val trade = trades[intent.offererId] ?: return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("No trade found for this player.", GameErrorCode.INVALID_TRADE)
        )

        if(!player.canDeductResources(trade.want)){
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES)
            )
        }
        // Notify original player offer of this player response
        sender.broadcast(
            GameplayEvent.TradeResponse(
                offererId = intent.offererId, // The original player that sent the trade
                accepted = intent.accepted,   // Whether they accepted or not
                senderId = userId             // The user that sent this response
            )
        )
    }

    private suspend fun handleTradeConfirmation(userId: PlayerId, intent: GameplayIntent.ConfirmTrade) {
        val trade = trades[userId] ?: return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("No trade found for this player.", GameErrorCode.INVALID_TRADE)
        )

        val player = players[userId]
        if(!player?.canDeductResources(trade.give)!!){
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES)
            )
        }

        //TODO Store and verify other player accepted

        sender.broadcast(
            GameplayEvent.TradeAccepted(
                responderId = intent.responderId, // The player that accepted the trade
                senderId = userId                 // The user that sent this response
            )
        )

        // Switch resources
        player.tryDeductResources(trade.give)
        player.addResources(trade.want)

        val changes =
            trade.give.mapValues { -it.value } + trade.want

        sender.sendToPlayer(
            userId,
            GameplayEvent.ResourcesUpdated(
                playerId = userId,
                changes = changes,
                reason = UpdateReason.TRADE
            )
        )

        val otherPlayer = players[intent.responderId]
        otherPlayer?.tryDeductResources(trade.want)
        otherPlayer?.addResources(trade.give)

        val otherChanges =
            trade.want.mapValues { -it.value } + trade.give

        sender.sendToPlayer(
            intent.responderId,
            GameplayEvent.ResourcesUpdated(
                playerId = userId,
                changes = otherChanges,
                reason = UpdateReason.TRADE
            )
        )
    }

    private suspend fun handleExchangeWithBank(userId: String, intent: GameplayIntent.ExchangeWithBank) {
        val player = players[userId] ?: return

        fun effectiveRatio(resourceId: String): Int {
            val specific = player.getPortDiscountRatio(resourceId)
            if (specific > 0) return specific

            val generic = player.getPortDiscountRatio(null)
            if (generic > 0) return generic

            return 4
        }

        var creditsRemaining = intent.get.values.sum()
        val toDeduct = mutableMapOf<String, Int>()

        for ((resourceId, offered) in intent.give) {
            if (creditsRemaining <= 0) break

            val ratio = effectiveRatio(resourceId)
            val creditsFromResource = offered / ratio
            if (creditsFromResource <= 0) continue

            val creditsUsed = minOf(creditsFromResource, creditsRemaining)
            toDeduct[resourceId] = creditsUsed * ratio
            creditsRemaining -= creditsUsed
        }

        if (creditsRemaining > 0) {
            sender.sendToPlayer(
                userId,
                GameplayEvent.GameError(
                    "Insufficient trade value.",
                    GameErrorCode.INSUFFICIENT_RESOURCES
                )
            )
            return
        }

        if (!player.canDeductResources(toDeduct)) {
            sender.sendToPlayer(
                userId,
                GameplayEvent.GameError(
                    "Insufficient resources.",
                    GameErrorCode.INSUFFICIENT_RESOURCES
                )
            )
            return
        }

        player.tryDeductResources(toDeduct)
        player.addResources(intent.get)

        val changes =
            toDeduct.mapValues { -it.value } + intent.get

        sender.sendToPlayer(
            userId,
            GameplayEvent.ResourcesUpdated(
                playerId = userId,
                changes = changes,
                reason = UpdateReason.TRADE
            )
        )
    }

}