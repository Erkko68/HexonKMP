package eric.bitria.hexon.viewmodel.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import eric.bitria.hexon.data.repository.GameRepository
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
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.render.GameCommand.DiceRolled
import eric.bitria.hexon.render.GameCommand.PlaceBuilding
import eric.bitria.hexon.render.GameCommand.SetHex
import eric.bitria.hexon.render.GameCommand.SetPort
import eric.bitria.hexon.render.GameCommand.ShowEdgeBuildingPositions
import eric.bitria.hexon.render.GameCommand.ShowVertexBuildingPositions
import eric.bitria.hexon.render.GameEvent
import eric.bitria.hexon.render.RenderEvent
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import eric.bitria.hexon.ws.LobbyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GameViewModel"

class GameViewModel(
    private val repository: GameRepository,
    private val sceneViewModel: GameSceneViewModel
) : ViewModel() {

    // --- UI States ---
    private val _turnPhase = MutableStateFlow(TurnPhase.WAITING)
    val turnPhase: StateFlow<TurnPhase> = _turnPhase

    // --- Definitions (Derived from Config) ---
    private val _gameConfig = MutableStateFlow<GameConfig?>(null)

    val resourcesDef: StateFlow<List<ResourceDef>> = _gameConfig
        .map { it?.resourceDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val buildingsDef: StateFlow<List<BuildingDef>> = _gameConfig
        .map { it?.buildingDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Game Data ---
    private val _victoryPoints = MutableStateFlow(0)
    val victoryPoints: StateFlow<Int> = _victoryPoints

    private val _board = MutableStateFlow<Board?>(null)

    // My Full State (Private info visible only to me)
    private val _me = MutableStateFlow<GamePlayer?>(null)
    val me: StateFlow<GamePlayer?> = _me

    // --- Trade ---
    private val _offeredResources = MutableStateFlow<Map<ResourceId, Int>>(emptyMap())
    val offeredResources: StateFlow<Map<ResourceId, Int>> = _offeredResources

    private val _requestedResources = MutableStateFlow<Map<ResourceId, Int>>(emptyMap())
    val requestedResources: StateFlow<Map<ResourceId, Int>> = _requestedResources

    // Opponents State (Public info: name, color, card counts, VPs)
    private val _opponents = MutableStateFlow<Map<PlayerId, PlayerSnapshot>>(emptyMap())
    val opponents: StateFlow<Map<PlayerId, PlayerSnapshot>> = _opponents

    private val _activePlayerId = MutableStateFlow<PlayerId?>(null)
    val activePlayerId: StateFlow<PlayerId?> = _activePlayerId

    private val _trades = MutableStateFlow<Map<PlayerId, TradeOffer>>(emptyMap())
    val trades: StateFlow<Map<PlayerId, TradeOffer>> = _trades

    // Map of offererId -> Map of responderId -> accepted/declined/pending
    private val _tradeResponses = MutableStateFlow<Map<PlayerId, Map<PlayerId, Boolean?>>>(emptyMap())
    val tradeResponses: StateFlow<Map<PlayerId, Map<PlayerId, Boolean?>>> = _tradeResponses

    // My active trade offer (if I'm the offerer)
    private val _myTradeOffer = MutableStateFlow<TradeOffer?>(null)
    val myTradeOffer: StateFlow<TradeOffer?> = _myTradeOffer

    init {
        Logger.d(TAG) { "GameViewModel init started" }

        // Listen to render events (engine lifecycle)
        viewModelScope.launch {
            Logger.d(TAG) { "Starting render events collection" }
            sceneViewModel.renderEvents.collect { event ->
                Logger.d(TAG) { "Received render event: ${event::class.simpleName}" }
                handleRenderEvent(event)
            }
        }

        // Listen to game events (user interactions)
        viewModelScope.launch {
            Logger.d(TAG) { "Starting game events collection" }
            sceneViewModel.gameEvents.collect { event ->
                Logger.d(TAG) { "Received game event: ${event::class.simpleName}" }
                handleGameEvent(event)
            }
        }

        // Listen to the Repository's Flow
        viewModelScope.launch {
            Logger.d(TAG) { "Starting repository incoming messages collection" }
            repository.incomingMessages.collect { message ->
                Logger.d(TAG) { "Received repository message: ${message::class.simpleName}" }
                when (message) {
                    is GameplayEvent -> {
                        Logger.d(TAG) { "Handling GameplayEvent: ${message::class.simpleName}" }
                        handleGameplayEvent(message)
                    }
                    is LobbyEvent -> {
                        // GameViewModel might receive late LobbyEvents if it starts before
                        // MatchmakingViewModel fully stops - just ignore them
                        Logger.d(TAG) { "Ignoring LobbyEvent in GameViewModel: ${message::class.simpleName}" }
                    }
                    else -> {
                        Logger.w(TAG) { "Unknown message type: ${message::class.simpleName}" }
                    }
                }
            }
        }

        Logger.d(TAG) { "GameViewModel init completed" }
    }

    // Read Messages Emitted by the Render Engine
    private fun handleRenderEvent(event: RenderEvent) {
        when (event) {
            is RenderEvent.Initialised -> {
                // The engine initialized/reinitialized, sync Board
                _board.value?.let(::syncScene)
            }
        }
    }

    // Read Messages Emitted by the Scene (User Interactions)
    private fun handleGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.PlacedBuilding -> {
                onBuildingPlaced(event.buildingId, event.hexA, event.hexB, event.hexC)
            }
        }
    }

    // Logic

    private fun handleGameplayEvent(event: GameplayEvent) {
        Logger.d(TAG) { "handleGameplayEvent() called with: ${event::class.simpleName}" }
        when (event) {
            // Game Flow
            is GameplayEvent.GameConfigLoaded -> {
                Logger.d(TAG) { "Handling GameConfigLoaded" }
                initializeGame(event.config)
            }
            is GameplayEvent.PlayerJoined -> {
                Logger.d(TAG) { "Handling PlayerJoined: ${event.player.id}" }
                addOpponent(event.player)
            }
            is GameplayEvent.GamePlayerStats -> {
                Logger.d(TAG) { "Handling GamePlayerStats" }
                updateMyStats(event.player)
            }
            is GameplayEvent.TurnChanged -> {
                Logger.d(TAG) { "Handling TurnChanged: newPlayer=${event.newPlayerId}, phase=${event.turnPhase}" }
                onTurnChanged(event)
            }
            is GameplayEvent.GameSnapshot -> {
                Logger.d(TAG) { "Handling GameSnapshot" }
                onGameSnapshot(event)
            }

            // Resources
            is GameplayEvent.DiceRolled -> {
                Logger.d(TAG) { "Handling DiceRolled: ${event.values}" }
                handleDiceRolled(event)
            }
            is GameplayEvent.ResourcesUpdated -> {
                Logger.d(TAG) { "Handling ResourcesUpdated: ${event.changes}" }
                handleResourcesUpdated(event)
            }
            is GameplayEvent.ResourceCountUpdated -> {
                Logger.d(TAG) { "Handling ResourceCountUpdated: player=${event.playerId}, changes=${event.changes}" }
                handleResourceCountUpdated(event)
            }

            // Robber
            is GameplayEvent.RobberUpdated -> {
                Logger.d(TAG) { "Handling RobberUpdated" }
                onRobberUpdated(event)
            }

            // Buildings
            is GameplayEvent.ObjectBuilt -> {
                Logger.d(TAG) { "Handling ObjectBuilt: buildingId=${event.buildingId}, player=${event.playerId}" }
                handleObjectBuilt(event)
            }

            // Trade
            is GameplayEvent.TradeProposed -> {
                Logger.d(TAG) { "Handling TradeProposed from ${event.offererId}" }
                onTradeProposed(event)
            }
            is GameplayEvent.TradeResponse -> {
                Logger.d(TAG) { "Handling TradeResponse: accepted=${event.accepted}" }
                onTradeResponse(event)
            }
            is GameplayEvent.TradeCompleted -> {
                Logger.d(TAG) { "Handling TradeCompleted" }
                onTradeCompleted(event)
            }
            is GameplayEvent.TradeCancelled -> {
                Logger.d(TAG) { "Handling TradeCancelled" }
                onTradeCancelled(event)
            }

            // End Game
            is GameplayEvent.GameOver -> {
                Logger.d(TAG) { "Handling GameOver" }
                onGameOver(event)
            }

            // Errors
            is GameplayEvent.GameError -> {
                Logger.e(TAG) { "Game Error: ${event.message}" }
            }
        }
    }

    // --- State Updaters ---

    private fun initializeGame(config: GameConfig) {
        Logger.d(TAG) { "initializeGame() called" }
        Logger.d(TAG) { "Config: victoryPoints=${config.victoryPoints}, resources=${config.resourceDefs.size}, buildings=${config.buildingDefs.size}" }

        _gameConfig.value = config

        val boardInstance = Board().apply {
            initialize(config)
        }
        Logger.d(TAG) { "Board initialized with ${boardInstance.tiles.size} tiles" }

        _board.value = boardInstance
        _victoryPoints.value = config.victoryPoints

        syncScene(boardInstance)
        Logger.d(TAG) { "initializeGame() completed" }
    }

    private fun syncScene(board: Board) {
        Logger.d(TAG) { "syncScene() called" }
        var commandCount = 0

        // Tiles
        board.tiles.values.forEach { tile ->
            sceneViewModel.sendCommand(
                SetHex(
                    coord = tile.coordinate,
                    resource = tile.resourceId,
                    number = tile.numberToken
                )
            )
            commandCount++
        }
        Logger.d(TAG) { "Synced ${board.tiles.size} tiles" }

        // Buildings
        board.buildings.forEach { (location, building) ->

            val coords = when (building.def.type) {
                PlacementType.VERTEX -> HexCoord.fromVertexId(location).toList()
                PlacementType.EDGE   -> HexCoord.fromEdgeId(location).toList()
            }

            sceneViewModel.sendCommand(
                PlaceBuilding(
                    player = building.ownerId,
                    color = opponents.value[building.ownerId]?.color ?: (me.value?.color ?: "#FFFFFF"),
                    buildingId = building.def.id,
                    placementType = building.def.type,
                    hexA = coords[0],
                    hexB = coords[1],
                    hexC = coords.getOrNull(2)
                )
            )
            commandCount++
        }
        Logger.d(TAG) { "Synced ${board.buildings.size} buildings" }

        board.ports.values.forEach {
            sceneViewModel.sendCommand(SetPort(it))
            commandCount++
        }
        Logger.d(TAG) { "Synced ${board.ports.size} ports. Total commands sent: $commandCount" }
    }

    private fun onGameOver(event: GameplayEvent.GameOver) {
        _turnPhase.value = TurnPhase.GAME_OVER
        _activePlayerId.value = event.winnerId
        viewModelScope.launch {
            repository.disconnect()
        }
        Logger.d(TAG) { "Game over! Winner: ${event.winnerId}" }

    }

    private fun onTurnChanged(event: GameplayEvent.TurnChanged) {
        _activePlayerId.value = event.newPlayerId
        if(event.turnPhase == TurnPhase.SETUP){
            _turnPhase.value = TurnPhase.SETUP
            return
        }
        if (event.newPlayerId == _me.value?.id) {
            _turnPhase.value = TurnPhase.MAIN_PHASE
        } else {
            _turnPhase.value = TurnPhase.WAITING
        }
    }

    fun onEndTurn() {
        viewModelScope.launch {
            repository.sendMessage(GameplayIntent.EndTurn)
        }
    }

    fun onExitGame() {
        Logger.d(TAG) { "onExitGame() called" }

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
        _requestedResources.value = emptyMap()
        _offeredResources.value = emptyMap()
    }

    // Intent
    fun sendBankExchange(){
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

    val canSendBankExchangeBool: StateFlow<Boolean> = combine(
        me, offeredResources, requestedResources, _gameConfig
    ) { m, o, r, c ->
        val p = m ?: return@combine false
        val cfg = c ?: return@combine false
        r.any { it.value > 0 } && p.calculateBankExchangeCost(o, r, cfg.tradeRatio) != null
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val canSendPlayerTradeBool: StateFlow<Boolean> = combine(
        me, offeredResources, requestedResources
    ) { m, o, r ->
        m?.canProposeTrade(o, r) == true
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Event
    fun onTradeProposed(event: GameplayEvent.TradeProposed) {
        val newOffer = TradeOffer(event.give, event.want)

        // Simplifies map update using the plus (+) operator
        _trades.value += (event.offererId to newOffer)

        if (event.offererId == _me.value?.id) {
            _myTradeOffer.value = newOffer
            // Initialize all opponents with null (pending) responses
            val initialResponses = _opponents.value.keys.associateWith { null as Boolean? }
            _tradeResponses.value += (event.offererId to initialResponses)
        }
    }

    fun onTradeResponse(event: GameplayEvent.TradeResponse) {
        // 1. Get the existing map for this offerer
        val currentOfferResponses = _tradeResponses.value[event.offererId] ?: emptyMap()

        // 2. Create a new inner map with the new response
        val updatedOfferResponses = currentOfferResponses + (event.responderId to event.accepted)

        // 3. Update the top-level map with the new inner map
        _tradeResponses.value += (event.offererId to updatedOfferResponses)
    }

    fun onTradeCompleted(event: GameplayEvent.TradeCompleted) = clearTrade(event.offererId)

    fun onTradeCancelled(event: GameplayEvent.TradeCancelled) = clearTrade(event.offererId)

    private fun clearTrade(offererId: String) {
        _trades.value -= offererId
        _tradeResponses.value -= offererId

        if (offererId == _me.value?.id) {
            _myTradeOffer.value = null
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
        Logger.d(TAG) { "handleResourcesUpdated() called with changes: ${event.changes}" }
        val currentMe = _me.value

        if (currentMe == null) {
            Logger.w(TAG) { "Cannot update resources: _me.value is null" }
            return
        }

        // Create a NEW resources map with changes applied
        val newResources = currentMe.resources.toMutableMap()
        event.changes.forEach { (resId, amount) ->
            val current = newResources[resId] ?: 0
            val newValue = current + amount
            Logger.d(TAG) { "Resource $resId: $current + $amount = $newValue" }
            if (newValue <= 0) {
                newResources.remove(resId)
            } else {
                newResources[resId] = newValue
            }
        }

        // Create a new GamePlayer with the new resources map
        _me.value = currentMe.copy(resources = newResources)
        Logger.d(TAG) { "Resources updated successfully. New total: ${newResources.values.sum()}" }
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

    val placeableBuildings: StateFlow<Set<BuildingId>> =
        combine(me, buildingsDef) { player, defs ->
            if (player == null) return@combine emptySet()

            defs
                .asSequence() // small micro-optimization
                .filter { player.canDeductResources(it.cost) }
                .map { it.id }
                .toSet()
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptySet()
        )

    // Intent
    fun showAvailableBuildingPositions(buildingId: BuildingId) {
        val player = _me.value ?: return
        val boardInstance = _board.value ?: return
        val building = buildingsDef.value.firstOrNull { it.id == buildingId } ?: return

        val command = when (building.type) {
            PlacementType.EDGE -> {
                val locations = boardInstance.getAvailableEdgePlacements(player.id, building.id)
                ShowEdgeBuildingPositions(buildingId, locations)
            }
            PlacementType.VERTEX -> {
                val checkConnection = _turnPhase.value != TurnPhase.SETUP
                val locations = boardInstance.getAvailableVertexPlacements(player.id, building.id, checkConnection)
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
        val boardInstance = _board.value ?: return
        val def = buildingsDef.value.firstOrNull { it.id == event.buildingId } ?: return

        // Update Logical Board
        if (event.hexC != null) {
            val checkConnection = _turnPhase.value != TurnPhase.SETUP
            boardInstance.placeVertexBuilding(def.id, event.playerId, event.hexA, event.hexB, event.hexC!!,checkConnection)
        } else {
            boardInstance.placeEdgeBuilding(def.id, event.playerId, event.hexA, event.hexB)
        }

        // Send Command to Renderer
        val playerColor = if (event.playerId == _me.value?.id) {
            _me.value?.color ?: "#FFFFFF"
        } else {
            _opponents.value[event.playerId]?.color ?: "#FFFFFF"
        }

        sceneViewModel.sendCommand(PlaceBuilding(
            player = event.playerId,
            buildingId = event.buildingId,
            color = playerColor,
            placementType = def.type,
            hexA = event.hexA,
            hexB = event.hexB,
            hexC = event.hexC
        ))

        _board.value = boardInstance

        // Update victory points immutably if this is the current player
        if (event.playerId == _me.value?.id) {
            val currentMe = _me.value!!
            _me.value = currentMe.copy(
                victoryPoints = currentMe.victoryPoints + def.points
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}
