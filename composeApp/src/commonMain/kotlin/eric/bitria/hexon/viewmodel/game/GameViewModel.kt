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
import kotlinx.coroutines.flow.asStateFlow
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
    private val _turnPhase = MutableStateFlow(TurnPhase.WAITING)
    val turnPhase: StateFlow<TurnPhase> = _turnPhase.asStateFlow()

    // --- Definitions (Derived from Config) ---
    private val _gameConfig = MutableStateFlow<GameConfig?>(null)

    val resourcesDef: StateFlow<List<ResourceDef>> = _gameConfig
        .map { it?.resourceDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val buildingsDef: StateFlow<List<BuildingDef>> = _gameConfig
        .map { it?.buildingDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Game Data ---
    private val _maxVictoryPoints = MutableStateFlow(0)
    val victoryPoints: StateFlow<Int> = _maxVictoryPoints.asStateFlow()

    private val _board = MutableStateFlow<Board?>(null)
    val board: StateFlow<Board?> = _board.asStateFlow()

    // My Full State (Private info visible only to me)
    private val _me = MutableStateFlow<GamePlayer?>(null)
    val me: StateFlow<GamePlayer?> = _me.asStateFlow()

    // --- Trade ---

    private val _offeredResources = MutableStateFlow<Map<ResourceId, Int>>(emptyMap())
    val offeredResources: StateFlow<Map<ResourceId, Int>> = _offeredResources.asStateFlow()

    private val _requestedResources = MutableStateFlow<Map<ResourceId, Int>>(emptyMap())
    val requestedResources: StateFlow<Map<ResourceId, Int>> = _requestedResources.asStateFlow()


    // Opponents State (Public info: name, color, card counts, VPs)
    private val _opponents = MutableStateFlow<Map<PlayerId, PlayerSnapshot>>(emptyMap())
    val opponents: StateFlow<Map<PlayerId, PlayerSnapshot>> = _opponents.asStateFlow()

    private val _activePlayerId = MutableStateFlow<PlayerId?>(null)
    val activePlayerId: StateFlow<PlayerId?> = _activePlayerId.asStateFlow()

    private val _trades = MutableStateFlow<Map<PlayerId, TradeOffer>>(emptyMap())
    val trades: StateFlow<Map<PlayerId, TradeOffer>> = _trades.asStateFlow()

    private val _tradeResponses = MutableStateFlow<Map<PlayerId, Boolean>>(emptyMap())
    val tradeResponses: StateFlow<Map<PlayerId, Boolean>> = _tradeResponses.asStateFlow()

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
                _board.value?.let(::syncScene)
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

        val board = Board(config.resourceDefs, config.buildingDefs).apply {
            initialize(config)
        }

        _board.value = board
        _maxVictoryPoints.value = config.victoryPoints

        syncScene(board) // just render
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
        _activePlayerId.value = event.newPlayerId
        if (event.newPlayerId == _me.value?.id) {
            _turnPhase.value = TurnPhase.MAIN_PHASE
        } else {
            _turnPhase.value = TurnPhase.WAITING
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
        val player = _me.value ?: return

        val currentCount = _offeredResources.value[resourceId] ?: 0
        val available = player.resources[resourceId] ?: 0

        if (currentCount < available) {
            // Increment selected count
            _offeredResources.value = _offeredResources.value.toMutableMap().apply {
                put(resourceId, currentCount + 1)
            }
        }
    }

    fun onOfferedResourceDeselected(resourceId: ResourceId) {
        val currentCount = _offeredResources.value[resourceId] ?: 0
        if (currentCount > 0) {
            _offeredResources.value = _offeredResources.value.toMutableMap().apply {
                put(resourceId, currentCount - 1)
                if (this[resourceId] == 0) remove(resourceId)
            }
        }
    }

    fun onRequestedResourceSelected(resourceId: ResourceId) {
        _requestedResources.update { current ->
            val newCount = (current[resourceId] ?: 0) + 1
            current.toMutableMap().apply { put(resourceId, newCount) }
        }
    }

    fun onRequestedResourceDeselected(resourceId: ResourceId) {
        val currentCount = _requestedResources.value[resourceId] ?: 0
        if (currentCount > 0) {
            _requestedResources.value = _requestedResources.value.toMutableMap().apply {
                put(resourceId, currentCount - 1)
                if (this[resourceId] == 0) remove(resourceId)
            }
        }
    }

    fun switchTradePanel() {
        _turnPhase.value = when (_turnPhase.value) {
            TurnPhase.TRADE -> TurnPhase.MAIN_PHASE
            TurnPhase.MAIN_PHASE -> TurnPhase.TRADE
            else -> _turnPhase.value
        }
    }

    // Intent
    fun sendBankExchange() {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.ExchangeWithBank(
                    give = _offeredResources.value,
                    get = _requestedResources.value
                )
            )
        }
        _offeredResources.value = emptyMap()
        _requestedResources.value = emptyMap()
    }

    fun sendPlayerExchange() {
        viewModelScope.launch {
            repository.sendMessage(
                GameplayIntent.ProposeTrade(
                    give = _offeredResources.value,
                    want = _requestedResources.value
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
                    offererId = _me.value?.id ?: return@launch
                )
            )
        }
    }

    fun canSendBankExchange(): Boolean {
        val player = _me.value ?: return false
        val config = _gameConfig.value ?: return false

        val offered = _offeredResources.value
        val requested = _requestedResources.value

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
        val player = _me.value ?: return false
        return player.canProposeTrade(_offeredResources.value, _requestedResources.value)
    }

    // Event
    fun onTradeProposed(event: GameplayEvent.TradeProposed) {
        // Store new Trade
        _trades.value = _trades.value.toMutableMap().apply {
            put(event.offererId, TradeOffer(event.give, event.want))
        }
    }

    fun onTradeResponse(event: GameplayEvent.TradeResponse) {
        // Store new Trade
        _tradeResponses.value = _tradeResponses.value.toMutableMap().apply {
            put(event.offererId, event.accepted)
        }
    }

    fun onTradeCompleted(event: GameplayEvent.TradeCompleted){
        _trades.value = _trades.value.toMutableMap().apply {
            remove(event.offererId)
        }
    }

    fun onTradeCancelled(event: GameplayEvent.TradeCancelled){
        _trades.value = _trades.value.toMutableMap().apply {
            remove(event.offererId)
        }
    }

    // --- Players ---

    private fun addOpponent(snapshot: PlayerSnapshot) {
        if (snapshot.id == _me.value?.id) return
        val current = _opponents.value.toMutableMap()
        current[snapshot.id] = snapshot
        _opponents.value = current
    }

    private fun updateMyStats(updatedMe: GamePlayer) {
        _me.value = updatedMe
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
        val currentMe = _me.value ?: return
        currentMe.addResources(event.changes)
        _me.value = currentMe
    }

    private fun handleResourceCountUpdated(event: GameplayEvent.ResourceCountUpdated) {
        val currentMap = _opponents.value
        val snapshot = currentMap[event.playerId] ?: return
        val updatedSnapshot = snapshot.copy(
            resourceCount = snapshot.resourceCount + event.changes
        )
        _opponents.value = currentMap.toMutableMap().apply {
            put(event.playerId, updatedSnapshot)
        }
    }


    // --- Buildings ---

    // Intent
    fun showAvailableBuildingPositions(buildingId: BuildingId) {
        val player = _me.value ?: return
        val board = _board.value ?: return
        val building = buildingsDef.value.firstOrNull { it.id == buildingId } ?: return

        val command = when (building.type) {
            PlacementType.EDGE -> {
                val locations = board.getAvailableEdgePlacements(player.id, building.id)
                ShowEdgeBuildingPositions(buildingId, locations)
            }
            PlacementType.VERTEX -> {
                val locations = board.getAvailableVertexPlacements(player.id, building.id)
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
        val board = _board.value ?: return
        val def = buildingsDef.value.firstOrNull { it.id == event.buildingId } ?: return

        // Update Logical Board
        if (event.hexC != null) {
            board.placeVertexBuilding(def.id, event.playerId, event.hexA, event.hexB, event.hexC!!)
        } else {
            board.placeEdgeBuilding(def.id, event.playerId, event.hexA, event.hexB)
        }

        // Send Command to Renderer
        sceneViewModel.sendCommand(GameCommand.PlaceBuilding(
            player = event.playerId,
            buildingId = event.buildingId,
            hexA = event.hexA,
            hexB = event.hexB,
            hexC = event.hexC
        ))

        _board.value = board

        if (event.playerId == _me.value?.id) {
            _me.value!!.victoryPoints += def.points
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}