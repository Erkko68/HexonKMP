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
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.game.data.enums.UpdateReason
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.lobby.LobbyPlayer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.forEach

class GameEngineImpl(
    private val sessionId: String,
    private val gameConfig: GameConfig = GameConfigLoader.default(sessionId)
) : GameEngine {

    // Infrastructure
    private lateinit var sender: GameMessageSender
    private val mutex = Mutex()

    // Internal State
    private val players = mutableMapOf<String, GamePlayer>()
    private val board = Board()
    val buildings: Map<BuildingId, BuildingDef> =
        gameConfig.buildingDefs.associateBy { it.id }
    val resources: Map<ResourceId, ResourceDef> =
        gameConfig.resourceDefs.associateBy { it.id }
    private val trades = mutableMapOf<PlayerId,TradeOffer>()
    private val tradeAcceptances = mutableMapOf<PlayerId, MutableSet<PlayerId>>()

    // Game Loop State
    private val playerQueue = mutableListOf<String>()
    private val setupQueue = mutableListOf<String>()
    private var setupTurnIndex = 0
    private var turnIndex = 0
    private var currentTurnPlayerId: PlayerId = ""
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
                is GameplayIntent.CancelTrade -> return handleTradeCancellation(userId)
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
                is GameplayIntent.EndTurn -> handleEndTurn()
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

        // Init Setup Queue (Snake Draft: 1..N, N..1)
        setupQueue.clear()
        setupQueue.addAll(playerQueue)
        setupQueue.addAll(playerQueue.reversed())

        setupTurnIndex = 0
        currentPhase = TurnPhase.SETUP
        currentTurnPlayerId = setupQueue[setupTurnIndex]

        // Grant resources for initial buildings
        grantInitialResources(currentTurnPlayerId)

        // Notify first player
        sender.broadcast(GameplayEvent.TurnChanged(newPlayerId = currentTurnPlayerId, turnPhase = TurnPhase.SETUP))
    }

    private suspend fun grantInitialResources(playerId: String) {
        val buildingIds = gameConfig.initialBuildings
        val toAdd = mutableMapOf<ResourceId, Int>()

        buildingIds.forEach { bId ->
            buildings[bId]?.cost?.forEach { (res, amt) ->
                toAdd[res] = (toAdd[res] ?: 0) + amt
            }
        }

        players[playerId]?.addResources(toAdd)
        notifyResourceChanges(playerId, toAdd, UpdateReason.PRODUCTION)
    }

    private suspend fun handleEndTurn() {
        if (currentPhase == TurnPhase.SETUP) {
            handleSetupEndTurn()
        } else {
            handleMainEndTurn()
        }
    }

    private suspend fun handleSetupEndTurn() {
        val player = players[currentTurnPlayerId] ?: return

        // Ensure player used all resources (placed buildings)
        if (player.totalResourceCount() > 0) {
            return sender.sendToPlayer(
                currentTurnPlayerId,
                GameplayEvent.GameError("You must place all initial buildings.", GameErrorCode.INVALID_PLACEMENT)
            )
        }

        setupTurnIndex++

        if (setupTurnIndex >= setupQueue.size) {
            // Setup Complete -> Main Phase
            currentPhase = TurnPhase.MAIN_PHASE
            turnIndex = -1 // Prepare for handleMainEndTurn to increment to 0
            handleMainEndTurn()
        } else {
            // Next Setup Turn
            currentTurnPlayerId = setupQueue[setupTurnIndex]
            grantInitialResources(currentTurnPlayerId)
            sender.broadcast(GameplayEvent.TurnChanged(newPlayerId = currentTurnPlayerId, turnPhase = TurnPhase.SETUP))
        }
    }

    private suspend fun handleMainEndTurn(){
        // 1. Calculate Next Player
        turnIndex = (turnIndex + 1) % playerQueue.size
        currentTurnPlayerId = playerQueue.elementAt(turnIndex)

        // 2. Logic: Generate Random Number
        val roll1 = (1..6).random()
        val roll2 = (1..6).random()
        val total = roll1 + roll2
        var production = mapOf<String, MutableMap<ResourceId, Int>>()

        // 3. Logic: Calculate Dice Resources
        currentPhase = if (total == 7) {
            TurnPhase.ROBBER_RESOLUTION
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

        // 3. Notify New Turn
        sender.broadcast(GameplayEvent.TurnChanged(newPlayerId = currentTurnPlayerId, turnPhase = currentPhase))

        production.forEach { (playerId, resources) ->
            // Store Player Resources
            players[playerId]?.addResources(resources)

            // Notify using the helper
            notifyResourceChanges(playerId, resources, UpdateReason.PRODUCTION)
        }
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
                val h3 = intent.h3 ?: return sender.sendToPlayer(
                    userId, GameplayEvent.GameError("Vertex building requires 3 coordinates", GameErrorCode.INVALID_PLACEMENT)
                )

                // Allow placement anywhere during setup (no road connection needed)
                val checkConnection = currentPhase != TurnPhase.SETUP

                // Validate Rules
                if (!board.canPlaceVertexBuilding(userId, intent.h1, intent.h2, h3, def.id, checkConnection)) {
                    return sender.sendToPlayer(userId, GameplayEvent.GameError("Invalid placement rule", GameErrorCode.INVALID_PLACEMENT))
                }
                board.placeVertexBuilding(def.id, userId, intent.h1, intent.h2, h3, checkConnection)
            }
            PlacementType.EDGE -> {
                // Validate Rules
                if (!board.canPlaceEdgeBuilding(userId, intent.h1, intent.h2, def.id)) {
                    return sender.sendToPlayer(userId, GameplayEvent.GameError("Invalid placement rule", GameErrorCode.INVALID_PLACEMENT))
                }
                board.placeEdgeBuilding(def.id, userId, intent.h1, intent.h2)
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
                hexA = intent.h1,
                hexB = intent.h2,
                hexC = intent.h3
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

    // --- Trade ---

    private suspend fun handleTradeProposal(userId: PlayerId, intent: GameplayIntent.ProposeTrade) {
        if (currentPhase == TurnPhase.SETUP) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Trading disabled during setup.", GameErrorCode.INVALID_TRADE))
        }

        val player = players[userId] ?: return

        if (!player.canProposeTrade(intent.give, intent.want)) {
            sender.sendToPlayer(userId, GameplayEvent.GameError("Invalid trade proposal", GameErrorCode.INVALID_TRADE))
            return
        }

        // Store new trade for the player and initialize set for players response
        trades[userId] = TradeOffer(
            give = intent.give,
            want = intent.want
        )
        tradeAcceptances[userId] = mutableSetOf()

        // Send trade proposal to the receiver
        sender.broadcast(
            GameplayEvent.TradeProposed(
                give = intent.give,
                want = intent.want,
                offererId = userId // Need to notify from who this trade is coming from
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
                responderId = userId,         // The player that responded
            )
        )
    }

    private suspend fun handleTradeConfirmation(userId: PlayerId, intent: GameplayIntent.ConfirmTrade) {
        val offerer = players[userId] ?: return
        val responder = players[intent.responderId] ?: return

        // 1. Validation: Self-trading is impossible here
        if (intent.responderId == userId) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Cannot trade with yourself.", GameErrorCode.INVALID_TRADE))
        }

        // 2. Retrieve the active trade
        val trade = trades[userId] ?: return sender.sendToPlayer(
            userId,
            GameplayEvent.GameError("No active trade found.", GameErrorCode.INVALID_TRADE)
        )

        // 3. Verify the responder actually accepted
        val acceptedPlayers = tradeAcceptances[userId] ?: mutableSetOf()
        if (!acceptedPlayers.contains(intent.responderId)) {
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("Player ${intent.responderId} has not accepted this trade.", GameErrorCode.INVALID_TRADE)
            )
        }

        // 4. Resource Verification (Double Check)
        if (!offerer.canDeductResources(trade.give)) {
            return sender.sendToPlayer(userId, GameplayEvent.GameError("You lack the resources.", GameErrorCode.INSUFFICIENT_RESOURCES))
        }
        if (!responder.canDeductResources(trade.want)) {
            // Remove invalid responder and notify offerer
            acceptedPlayers.remove(intent.responderId)
            return sender.sendToPlayer(userId, GameplayEvent.GameError("Responder lacks resources. Offer removed.", GameErrorCode.INSUFFICIENT_RESOURCES))
        }

        // 5. Execute Swap
        // Offerer: -Give, +Want
        offerer.tryDeductResources(trade.give)
        offerer.addResources(trade.want)

        // Responder: -Want, +Give
        responder.tryDeductResources(trade.want)
        responder.addResources(trade.give)

        // 6. Cleanup & Notify
        trades.remove(userId)
        tradeAcceptances.remove(userId)

        // Broadcast completion so clients update UI (hide trade windows, show animation)
        sender.broadcast(
            GameplayEvent.TradeCompleted(
                responderId = intent.responderId,
                offererId = userId
            )
        )

        // 7. Send Private Inventory Updates
        val offererChanges = trade.give.mapValues { -it.value } + trade.want
        notifyResourceChanges(userId, offererChanges, UpdateReason.TRADE)

        val responderChanges = trade.want.mapValues { -it.value } + trade.give
        notifyResourceChanges(intent.responderId, responderChanges, UpdateReason.TRADE)
    }

    private suspend fun handleTradeCancellation(userId: PlayerId) {
        // 1. Check if they actually have an active trade
        if (!trades.containsKey(userId)) {
            return sender.sendToPlayer(
                userId,
                GameplayEvent.GameError("No active trade to cancel.", GameErrorCode.INVALID_TRADE)
            )
        }

        // 2. Remove the trade and acceptances
        trades.remove(userId)
        tradeAcceptances.remove(userId)

        // 3. Notify all clients to close the trade window/remove the icon
        sender.broadcast(
            GameplayEvent.TradeCancelled(
                offererId = userId,
            )
        )
    }

    private suspend fun handleExchangeWithBank(userId: String, intent: GameplayIntent.ExchangeWithBank) {
        val player = players[userId] ?: return

        // Pass gameConfig.tradeRatio here
        val toDeduct = player.calculateBankExchangeCost(
            give = intent.give,
            get = intent.get,
            defaultRatio = gameConfig.tradeRatio
        )

        if (toDeduct == null) {
            sender.sendToPlayer(userId, GameplayEvent.GameError("Insufficient trade value.", GameErrorCode.INSUFFICIENT_RESOURCES))
            return
        }

        if (!player.canDeductResources(toDeduct)) {
            sender.sendToPlayer(userId, GameplayEvent.GameError("Insufficient resources.", GameErrorCode.INSUFFICIENT_RESOURCES))
            return
        }

        player.tryDeductResources(toDeduct)
        player.addResources(intent.get)

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