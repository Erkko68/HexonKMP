package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.Board
import eric.bitria.hexon.game.GameConfigLoader
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.def.ResourceDef
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.enums.UpdateReason
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.lobby.LobbyPlayer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.forEach

private enum class TurnPhase {
    SETUP,          // Initial settlement placement (Snake draft)
    MAIN_PHASE,     // Can build, trade, play cards
    ROBBER_RESOLUTION, // Waiting for a player to move the robber
    GAME_OVER
}

class GameEngineImpl(
    private val sessionId: String,
    private val gameConfig: GameConfig = GameConfigLoader.default(sessionId)
) : GameEngine {

    // Infrastructure
    private lateinit var sender: GameMessageSender
    private val mutex = Mutex()

    // Internal State
    private val players = mutableMapOf<String, GamePlayer>()
    private val board = Board(
        availableResources = gameConfig.resourceDefs,
        availableBuildings = gameConfig.buildingDefs
    )
    val buildings: Map<BuildingId, BuildingDef> =
        gameConfig.buildingDefs.associateBy { it.id }
    val resources: Map<ResourceId, ResourceDef> =
        gameConfig.resourceDefs.associateBy { it.id }
    private val trades = mutableMapOf<PlayerId, TradeOffer>()
    private val tradeAcceptances = mutableMapOf<PlayerId, MutableSet<PlayerId>>()

    // Game Loop State
    private val playerQueue = mutableListOf<String>()
    private var turnIndex = 0
    private var currentTurnPlayerId: String = ""
    private var currentPhase: TurnPhase = TurnPhase.SETUP

    override suspend fun start(lobbyPlayers: List<LobbyPlayer>, sender: GameMessageSender) {
        mutex.withLock {
            this.sender = sender

            // 0. Map Generation (Procedural Logic)
            board.initialize(gameConfig)

            // 1. Send Configuration & Initial State to Clients
            sender.broadcast(GameplayEvent.GameConfigLoaded(gameConfig))

            // 2. Initialize Players
            lobbyPlayers.forEach { player ->
                val gamePlayer = GamePlayer(player.id, player.name, player.color, player.isHost)
                players[player.id] = gamePlayer

                // Send snapshot of this new player to all others
                sender.broadcast(
                    GameplayEvent.PlayerJoined(gamePlayer.toSnapshot()),
                    excludeUserId = player.id
                )

                // Send full player state to new player
                sender.sendToPlayer(
                    player.id,
                    GameplayEvent.GamePlayerStats(gamePlayer)
                )
            }

            // 4. Kickoff the Loop
            startFirstTurn()
        }
    }

    override suspend fun onMessage(userId: String, message: GameMessage) {
        mutex.withLock {
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
    }

    override suspend fun onPlayerLeave(userId: String) {
        mutex.withLock {
            // Handle pause logic or cleanup safely
            // sender.broadcast(GameMessage.GameInfo("Player $userId disconnected"))
        }
    }

    override suspend fun onPlayerRejoin(userId: String) {
        mutex.withLock {
            // Handle pause logic or cleanup safely
            // sender.broadcast(GameMessage.GameInfo("Player $userId disconnected"))
        }
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
            )
        )

        production.forEach { (playerId, resources) ->
            // Store Player Resources
            players[playerId]?.addResources(resources)

            // Notify using the helper
            notifyResourceChanges(playerId, resources, UpdateReason.PRODUCTION)
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

        // Notify resource deduction for building
        val deduction = def.cost.mapValues { -it.value }
        notifyResourceChanges(userId, deduction, UpdateReason.BUILD)

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

        // 1. Remove from victim
        players[victimId]?.tryDeductResources(stolenResource)
        notifyResourceChanges(
            victimId,
            stolenResource.mapValues { -it.value },
            UpdateReason.THEFT
        )

        // 2. Give to robber player
        players[userId]?.addResources(stolenResource)
        notifyResourceChanges(
            userId,
            stolenResource,
            UpdateReason.THEFT
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

        // Store new trade for the player and initialize set for players response
        trades[userId] = intent.offer
        tradeAcceptances[userId] = mutableSetOf()

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

        if (userId == intent.offererId) return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("Cannot respond to your own trade.", GameErrorCode.INVALID_TRADE)
        )

        if(!player.canDeductResources(trade.want)){
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Insufficient resources", GameErrorCode.INSUFFICIENT_RESOURCES)
            )
        }

        // 3. NEW: Update the acceptance state
        tradeAcceptances
            .getOrPut(intent.offererId) { mutableSetOf() }
            .apply {
                if (intent.accepted) add(userId) else remove(userId)
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
        val offerer = players[userId] ?: return
        val responder = players[intent.responderId] ?: return

        if (intent.responderId == userId) return sender.sendToPlayer(userId, GameplayEvent.GameError("Cannot confirm your own trade.", GameErrorCode.INVALID_TRADE))

        // 1. Retrieve the active trade
        val trade = trades[userId] ?: return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("No active trade found.", GameErrorCode.INVALID_TRADE)
        )

        // 2. Verify the responder actually accepted THIS trade
        val acceptedPlayers = tradeAcceptances[userId] ?: mutableSetOf()
        if (!acceptedPlayers.contains(intent.responderId)) {
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Player ${intent.responderId} has not accepted this trade.", GameErrorCode.INVALID_TRADE)
            )
        }

        // 3. Ensure enough resources
        if (!offerer.canDeductResources(trade.give)) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("You lack resources.", GameErrorCode.INSUFFICIENT_RESOURCES))
        }
        if (!responder.canDeductResources(trade.want)) {
            // Remove them from acceptances so the offerer knows they are invalid now
            acceptedPlayers.remove(intent.responderId)
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Responder lacks resources.", GameErrorCode.INSUFFICIENT_RESOURCES))
        }

        // 4. Execute Swap

        // Deduct from Offerer / Give to Responder
        offerer.tryDeductResources(trade.give)
        responder.addResources(trade.give)

        // Deduct from Responder / Give to Offerer
        responder.tryDeductResources(trade.want)
        offerer.addResources(trade.want)

        // 5. Cleanup
        trades.remove(userId)
        tradeAcceptances.remove(userId)

        // 6. Notify
        sender.broadcast(
            GameplayEvent.TradeAccepted(
                responderId = intent.responderId,
                senderId = userId
            )
        )

        // 7. Send Updates using helper

        // Offerer lost 'give' and gained 'want'
        val offererChanges = trade.give.mapValues { -it.value } + trade.want
        notifyResourceChanges(userId, offererChanges, UpdateReason.TRADE)

        // Responder lost 'want' and gained 'give'
        val responderChanges = trade.want.mapValues { -it.value } + trade.give
        notifyResourceChanges(intent.responderId, responderChanges, UpdateReason.TRADE)
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

        // Map deductions to negative values for the change log
        val changes = toDeduct.mapValues { -it.value } + intent.get

        notifyResourceChanges(userId, changes, UpdateReason.TRADE)
    }


    // --- Helper Function for Resource Notifications ---

    /**
     * Sends private details to the affected player and public counts to everyone else.
     * @param playerId The player whose resources changed.
     * @param changes Map of resource ID to amount (positive for gain, negative for loss).
     * @param reason The reason for the update.
     */
    private suspend fun notifyResourceChanges(
        playerId: PlayerId,
        changes: Map<ResourceId, Int>,
        reason: UpdateReason
    ) {
        // 1. Send detailed update (Specific Cards) to the Player
        sender.sendToPlayer(
            playerId,
            GameplayEvent.ResourcesUpdated(
                changes = changes,
                reason = reason
            )
        )

        // 2. Send generic update (Card Count) to Everyone Else
        val netChange = changes.values.sum()
        if (netChange != 0) {
            sender.broadcast(
                GameplayEvent.ResourceCountUpdated(
                    playerId = playerId,
                    changes = netChange,
                    reason = reason
                ),
                excludeUserId = playerId
            )
        }
    }

}