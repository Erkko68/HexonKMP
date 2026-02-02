package eric.bitria.hexon.viewmodel.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.repository.GameRepository
import eric.bitria.hexon.game.Board
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlayerSnapshot
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.ResourceDef
import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.render.GameEvent
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayEvent
import eric.bitria.hexon.ws.GameplayIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val _board = MutableStateFlow<Board?>(null)
    val board: StateFlow<Board?> = _board.asStateFlow()

    // My Full State (Private info visible only to me)
    private val _me = MutableStateFlow<GamePlayer?>(null)
    val me: StateFlow<GamePlayer?> = _me.asStateFlow()

    // Opponents State (Public info: name, color, card counts, VPs)
    private val _opponents = MutableStateFlow<Map<String, PlayerSnapshot>>(emptyMap())
    val opponents: StateFlow<Map<String, PlayerSnapshot>> = _opponents.asStateFlow()

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

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
                handleMessage(message)
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
        }
    }

    // Read Messages Emitted by the Server
    private fun handleMessage(message: GameMessage) {
        if (message is GameplayEvent) {
            handleGameplayEvent(message)
        }
    }

    // Logic

    private fun handleGameplayEvent(event: GameplayEvent) {
        when (event) {
            is GameplayEvent.GameConfigLoaded -> initializeGame(event.config)
            is GameplayEvent.PlayerJoined -> addOpponent(event.player)
            is GameplayEvent.GamePlayerStats -> updateMyStats(event.player)
            is GameplayEvent.TurnChanged -> {
                _activePlayerId.value = event.newPlayerId
                if (event.newPlayerId == _me.value?.id) {
                    _turnPhase.value = TurnPhase.MAIN_PHASE
                } else {
                    _turnPhase.value = TurnPhase.WAITING
                }
            }
            is GameplayEvent.ResourcesUpdated -> handleResourcesUpdated(event)
            is GameplayEvent.ResourceCountUpdated -> handleResourceCountUpdated(event)
            is GameplayEvent.ObjectBuilt -> handleObjectBuilt(event)
            is GameplayEvent.DiceRolled -> {
                 sceneViewModel.sendCommand(GameCommand.DiceRolled(
                     values = event.values,
                     sum = event.values.first + event.values.second
                 ))
            }
            is GameplayEvent.GameError -> {
                println("Game Error: ${event.message}")
            }
            else -> {}
        }
    }

    // --- State Updaters ---

    private fun initializeGame(config: GameConfig) {
        _gameConfig.value = config

        val board = Board(config.resourceDefs, config.buildingDefs).apply {
            initialize(config)
        }

        _board.value = board

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


    // Game UI

    private fun addOpponent(snapshot: PlayerSnapshot) {
        if (snapshot.id == _me.value?.id) return
        val current = _opponents.value.toMutableMap()
        current[snapshot.id] = snapshot
        _opponents.value = current
    }

    private fun updateMyStats(updatedMe: GamePlayer) {
        _me.value = updatedMe
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