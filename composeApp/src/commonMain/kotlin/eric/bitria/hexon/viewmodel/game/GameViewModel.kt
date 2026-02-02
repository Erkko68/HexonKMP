package eric.bitria.hexon.viewmodel.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.repository.GameRepository
import eric.bitria.hexon.game.Board
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.PlayerSnapshot
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.ResourceDef
import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.render.GameCommand.*
import eric.bitria.hexon.render.GameEvent
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TurnPhase {
    SETUP,          // Initial settlement placement
    WAITING,        // Waiting for server/other players
    TRADE,
    MAIN_PHASE,     // Active turn (Build, Trade)
    ROBBER_RESOLUTION, // Discarding or moving robber
    GAME_OVER
}

class GameViewModel(
    private val repository: GameRepository,
    private val sceneViewModel: GameSceneViewModel
) : ViewModel() {

    // --- UI States ---
    val turnPhase: StateFlow<TurnPhase>
        field = MutableStateFlow(TurnPhase.WAITING)

    // --- Definitions (Derived from Config) ---
    private val _gameConfig = MutableStateFlow<GameConfig?>(null)

    val resourcesDef: StateFlow<List<ResourceDef>> = _gameConfig
        .map { it?.resourceDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val buildingsDef: StateFlow<List<BuildingDef>> = _gameConfig
        .map { it?.buildingDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Game Data ---
    val victoryPoints: StateFlow<Int>
        field = MutableStateFlow(0)

    val board: StateFlow<Board?>
        field = MutableStateFlow<Board?>(null)

    // My Full State (Private info visible only to me)
    val me: StateFlow<GamePlayer?>
        field = MutableStateFlow<GamePlayer?>(null)

    // --- Trade ---

    val offeredResources: StateFlow<Map<ResourceId, Int>>
        field = MutableStateFlow(emptyMap())

    val requestedResources: StateFlow<Map<ResourceId, Int>>
        field = MutableStateFlow(emptyMap())


    // Opponents State (Public info: name, color, card counts, VPs)
    val opponents: StateFlow<Map<PlayerId, PlayerSnapshot>>
        field = MutableStateFlow(emptyMap())

    val activePlayerId: StateFlow<PlayerId?>
        field = MutableStateFlow<PlayerId?>(null)

    val trades: StateFlow<Map<PlayerId, TradeOffer>>
        field = MutableStateFlow(emptyMap())

    val tradeResponses: StateFlow<Map<PlayerId, Boolean?>>
        field = MutableStateFlow(emptyMap())

    init {
        // Listen to scene events
        viewModelScope.launch {
            sceneViewModel.gameEvents.collect { event ->
                handleSceneEvent(event)
            }
        }

        // Listen to the Repository's Flow
        viewModelScope.launch {
            repository.incomingMessages.collect { message ->
                if (message is GameplayEvent) {
                    handleGameplayEvent(message)
                }
            }
        }
    }

    // Read Messages Emitted by the Scene
    private fun handleSceneEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Initialised -> {
                // The engine reinitialized, sync Board
                board.value?.let(::syncScene)
            }
            is GameEvent.PlacedBuilding -> {
                onBuildingPlaced(event.buildingId, event.hexA, event.hexB, event.hexC)
            }
        }
    }

    // Logic

    private fun handleGameplayEvent(event: GameplayEvent) {
        when (event) {
            // Game Flow
            is GameplayEvent.GameConfigLoaded -> initializeGame(event.config)
            is GameplayEvent.PlayerJoined -> addOpponent(event.player)
            is GameplayEvent.GamePlayerStats -> updateMyStats(event.player)
            is GameplayEvent.TurnChanged -> onTurnChanged(event)
            is GameplayEvent.GameSnapshot -> onGameSnapshot(event)

            // Resources
            is GameplayEvent.DiceRolled -> handleDiceRolled(event)
            is GameplayEvent.ResourcesUpdated -> handleResourcesUpdated(event)
            is GameplayEvent.ResourceCountUpdated -> handleResourceCountUpdated(event)

            // Robber
            is GameplayEvent.RobberUpdated -> onRobberUpdated(event)

            // Buildings
            is GameplayEvent.ObjectBuilt -> handleObjectBuilt(event)

            // Trade
            is GameplayEvent.TradeProposed -> onTradeProposed(event)
            is GameplayEvent.TradeResponse -> onTradeResponse(event)
            is GameplayEvent.TradeCompleted -> onTradeCompleted(event)
            is GameplayEvent.TradeCancelled -> onTradeCancelled(event)

            // End Game
            is GameplayEvent.GameOver -> TODO()

            // Errors
            is GameplayEvent.GameError -> {
                println("Game Error: ${event.message}")
            }
        }
    }

    // --- State Updaters ---

    private fun initializeGame(config: GameConfig) {
        _gameConfig.value = config

        val boardInstance = Board(config.resourceDefs, config.buildingDefs).apply {
            initialize(config)
        }

        board.value = boardInstance
        victoryPoints.value = config.victoryPoints

        syncScene(boardInstance) // just render
    }

    private fun syncScene(board: Board) {
        // Tiles
        board.tiles.values.forEach { tile ->
            sceneViewModel.sendCommand(
                GameCommand.SetHex(
                    coord = tile.coordinate,
                    resource = tile.resourceId,
                    number = tile.numberToken
                )
            )
        }

        // Buildings
        board.buildings.forEach { (location, building) ->

            val coords = when (building.def.type) {
                PlacementType.VERTEX -> HexCoord.fromVertexId(location).toList()
                PlacementType.EDGE   -> HexCoord.fromEdgeId(location).toList()
            }

            sceneViewModel.sendCommand(
                GameCommand.PlaceBuilding(
                    player = building.ownerId,
                    buildingId = building.def.id,
                    hexA = coords[0],
                    hexB = coords[1],
                    hexC = coords.getOrNull(2)
                )
            )
        }

        board.ports.values.forEach {
            sceneViewModel.sendCommand(GameCommand.SetPort(it))
        }
    }

    private fun onTurnChanged(event: GameplayEvent.TurnChanged) {
        activePlayerId.value = event.newPlayerId
        if (event.newPlayerId == me.value?.id) {
            turnPhase.value = TurnPhase.MAIN_PHASE
        } else {
            turnPhase.value = TurnPhase.WAITING
        }
    }

    fun onEndTurn() {
        viewModelScope.launch {
            repository.sendMessage(GameplayIntent.EndTurn)
        }
    }

    fun onExitGame() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    private fun onGameSnapshot(event: GameplayEvent.GameSnapshot) {
        // TODO for both Client and Server
    }

    // --- Robber ---
    private fun onRobberUpdated(event: GameplayEvent.RobberUpdated){

    }

    // --- Trade ---

    // UI
    fun onOfferedResourceSelected(resourceId: ResourceId) {
        val player = me.value ?: return

        val currentCount = offeredResources.value[resourceId] ?: 0
        val available = player.resources[resourceId] ?: 0

        if (currentCount < available) {
            // Increment selected count
            offeredResources.value = offeredResources.value.toMutableMap().apply {
                put(resourceId, currentCount + 1)
            }
        }
    }

    fun onOfferedResourceDeselected(resourceId: ResourceId) {
        val currentCount = offeredResources.value[resourceId] ?: 0
        if (currentCount > 0) {
            offeredResources.value = offeredResources.value.toMutableMap().apply {
                put(resourceId, currentCount - 1)
                if (this[resourceId] == 0) remove(resourceId)
            }
        }
    }

    fun onRequestedResourceSelected(resourceId: ResourceId) {
        requestedResources.update { current ->
            val newCount = (current[resourceId] ?: 0) + 1
            current.toMutableMap().apply { put(resourceId, newCount) }
        }
    }

    fun onRequestedResourceDeselected(resourceId: ResourceId) {
        val currentCount = requestedResources.value[resourceId] ?: 0
        if (currentCount > 0) {
            requestedResources.value = requestedResources.value.toMutableMap().apply {
                put(resourceId, currentCount - 1)
                if (this[resourceId] == 0) remove(resourceId)
            }
        }
    }

    fun switchTradePanel() {
        turnPhase.value = when (turnPhase.value) {
            TurnPhase.TRADE -> TurnPhase.MAIN_PHASE
            TurnPhase.MAIN_PHASE -> TurnPhase.TRADE
            else -> turnPhase.value
        }
    }

    // Intent
    fun sendBankExchange() {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.ExchangeWithBank(
                    give = offeredResources.value,
                    get = requestedResources.value
                )
            )
        }
        offeredResources.value = emptyMap()
        requestedResources.value = emptyMap()
    }

    fun sendPlayerExchange() {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.ProposeTrade(
                    give = offeredResources.value,
                    want = requestedResources.value
                )
            )
        }
    }

    /**
     * Respond to another player's trade offer
     */
    fun sendTradeResponse(player: PlayerId, accepted: Boolean) {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.RespondToTrade(
                    offererId = player,
                    accepted = accepted
                )
            )
        }
    }

    /**
     * Confirm other player accepted response on my trade
     */
    fun sendTradeConfirmation(responderId: PlayerId) {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.ConfirmTrade(
                    responderId = responderId,
                )
            )
        }
    }

    /**
     * Cancel my trade
     */
    fun sendCancelTrade() {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.CancelTrade(
                    offererId = me.value?.id ?: return@launch
                )
            )
        }
    }

    fun canSendBankExchange(): Boolean {
        val player = me.value ?: return false
        val config = _gameConfig.value ?: return false

        val offered = offeredResources.value
        val requested = requestedResources.value

        // 1. Basic validation: Must actually want something
        if (requested.isEmpty() || requested.values.sum() == 0) {
            return false
        }

        // 2. Use the shared math logic from GamePlayer
        // This returns NULL if the 'offered' value isn't high enough to buy the 'requested' items.
        val cost = player.calculateBankExchangeCost(
            give = offered,
            get = requested,
            defaultRatio = config.tradeRatio
        )

        return cost != null
    }

    fun canSendPlayerTrade(): Boolean {
        val player = me.value ?: return false
        return player.canProposeTrade(offeredResources.value, requestedResources.value)
    }

    // Event
    fun onTradeProposed(event: GameplayEvent.TradeProposed) {
        // Store new Trade
        trades.value = trades.value.toMutableMap().apply {
            put(event.offererId, TradeOffer(event.give, event.want))
        }
    }

    fun onTradeResponse(event: GameplayEvent.TradeResponse) {
        // Store new Trade
        tradeResponses.value = tradeResponses.value.toMutableMap().apply {
            put(event.offererId, event.accepted)
        }
    }

    fun onTradeCompleted(event: GameplayEvent.TradeCompleted){
        trades.value = trades.value.toMutableMap().apply {
            remove(event.offererId)
        }
    }

    fun onTradeCancelled(event: GameplayEvent.TradeCancelled){
        trades.value = trades.value.toMutableMap().apply {
            remove(event.offererId)
        }
    }

    // --- Players ---

    private fun addOpponent(snapshot: PlayerSnapshot) {
        if (snapshot.id == me.value?.id) return
        val current = opponents.value.toMutableMap()
        current[snapshot.id] = snapshot
        opponents.value = current
    }

    private fun updateMyStats(updatedMe: GamePlayer) {
        me.value = updatedMe
    }

    // --- Resources ---

    private fun handleDiceRolled(event: GameplayEvent.DiceRolled){
        sceneViewModel.sendCommand(
            DiceRolled(
                values = event.values,
                sum = event.values.first + event.values.second
            )
        )
    }

    private fun handleResourcesUpdated(event: GameplayEvent.ResourcesUpdated) {
        val currentMe = me.value ?: return
        currentMe.addResources(event.changes)
        me.value = currentMe
    }

    private fun handleResourceCountUpdated(event: GameplayEvent.ResourceCountUpdated) {
        val currentMap = opponents.value
        val snapshot = currentMap[event.playerId] ?: return
        val updatedSnapshot = snapshot.copy(
            resourceCount = snapshot.resourceCount + event.changes
        )
        opponents.value = currentMap.toMutableMap().apply {
            put(event.playerId, updatedSnapshot)
        }
    }


    // --- Buildings ---

    // Intent
    fun showAvailableBuildingPositions(buildingId: BuildingId) {
        val player = me.value ?: return
        val boardInstance = board.value ?: return
        val building = buildingsDef.value.firstOrNull { it.id == buildingId } ?: return

        val command = when (building.type) {
            PlacementType.EDGE -> {
                val locations = boardInstance.getAvailableEdgePlacements(player.id, building.id)
                ShowEdgeBuildingPositions(buildingId, locations)
            }
            PlacementType.VERTEX -> {
                val locations = boardInstance.getAvailableVertexPlacements(player.id, building.id)
                ShowVertexBuildingPositions(buildingId, locations)
            }
        }

        sceneViewModel.sendCommand(command)
    }

    fun onBuildingPlaced(
        buildingId: BuildingId,
        h1: HexCoord,
        h2: HexCoord,
        h3: HexCoord? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.Build(
                    buildingId = buildingId,
                    h1 = h1,
                    h2 = h2,
                    h3 = h3
                )
            )
        }
    }

    // Event
    private fun handleObjectBuilt(event: GameplayEvent.ObjectBuilt) {
        val boardInstance = board.value ?: return
        val def = buildingsDef.value.firstOrNull { it.id == event.buildingId } ?: return

        // Update Logical Board
        if (event.hexC != null) {
            boardInstance.placeVertexBuilding(def.id, event.playerId, event.hexA, event.hexB, event.hexC!!)
        } else {
            boardInstance.placeEdgeBuilding(def.id, event.playerId, event.hexA, event.hexB)
        }

        // Send Command to Renderer
        sceneViewModel.sendCommand(PlaceBuilding(
            player = event.playerId,
            buildingId = event.buildingId,
            hexA = event.hexA,
            hexB = event.hexB,
            hexC = event.hexC
        ))

        board.value = boardInstance

        if (event.playerId == me.value?.id) {
            me.value!!.victoryPoints += def.points
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}
