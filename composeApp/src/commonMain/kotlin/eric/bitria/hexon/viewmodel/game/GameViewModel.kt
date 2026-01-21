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
import eric.bitria.hexon.game.data.def.ResourceDef
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
    MAIN_PHASE,     // Active turn (Build, Trade)
    ROBBER_RESOLUTION, // Discarding or moving robber
    GAME_OVER
}

class GameViewModel(
    private val repository: GameRepository,
    private val sessionId: String
) : ViewModel() {

    // --- UI States ---
    private val _turnPhase = MutableStateFlow(TurnPhase.WAITING)
    val turnPhase: StateFlow<TurnPhase> = _turnPhase.asStateFlow()

    // --- Definitions (Derived from Config) ---
    // UI components can observe these to render the "Cost" cards or "Resource" icons
    private val _gameConfig = MutableStateFlow<GameConfig?>(null)

    val resourceDefs: StateFlow<List<ResourceDef>> = _gameConfig
        .map { it?.resourceDefs ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val buildingDefs: StateFlow<List<BuildingDef>> = _gameConfig
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
        // 1. Connect (Idempotent: if Matchmaking already connected, this just ensures it's active)
        viewModelScope.launch {
            try {
                repository.connect(sessionId)
            } catch (e: Exception) {
                println("Connection failed: ${e.message}")
                // Handle error (e.g. navigate back)
            }
        }

        // 2. Listen to the Repository's Hot Flow
        viewModelScope.launch {
            repository.incomingMessages.collect { message ->
                handleMessage(message)
            }
        }
    }

    // --- Message Handling ---

    private fun handleMessage(message: GameMessage) {
        if (message is GameplayEvent) {
            handleGameplayEvent(message)
        }
    }

    private fun handleGameplayEvent(event: GameplayEvent) {
        when (event) {
            is GameplayEvent.GameConfigLoaded -> initializeGame(event.config)

            // Opponent Joining (Public Info)
            is GameplayEvent.PlayerJoined -> addOpponent(event.player)

            // My Data (Private Info)
            is GameplayEvent.GamePlayerStats -> updateMyStats(event.player)

            is GameplayEvent.TurnChanged -> {
                _activePlayerId.value = event.newPlayerId
            }

            // --- Gameplay Updates ---
            is GameplayEvent.ResourcesUpdated -> handleResourcesUpdated(event)
            is GameplayEvent.ObjectBuilt -> handleObjectBuilt(event)
            is GameplayEvent.DiceRolled -> { /* Trigger Dice Animation in UI */ }

            is GameplayEvent.GameError -> {
                println("Game Error: ${event.message}")
                // Trigger UI Snackbar
            }
            else -> {}
        }
    }

    // --- State Updaters ---

    private fun initializeGame(config: GameConfig) {
        _gameConfig.value = config

        // Deterministic Board Generation
        val clientBoard = Board(config.resourceDefs, config.buildingDefs)
        clientBoard.initialize(config)

        _board.value = clientBoard
        _turnPhase.value = TurnPhase.SETUP
    }

    private fun addOpponent(snapshot: PlayerSnapshot) {
        // If the snapshot is ME, ignore it (we use GamePlayer for me)
        if (snapshot.id == _me.value?.id) return

        val current = _opponents.value.toMutableMap()
        current[snapshot.id] = snapshot
        _opponents.value = current
    }

    private fun updateMyStats(updatedMe: GamePlayer) {
        _me.value = updatedMe
    }

    private fun handleResourcesUpdated(event: GameplayEvent.ResourcesUpdated) {
        // If it's me, the server sends a GamePlayerStats update usually,
        // but if we receive partial updates, we apply them here.
        if (event.playerId == _me.value?.id) {
            val currentMe = _me.value ?: return
            currentMe.addResources(event.changes) // Updates internal map
            _me.value = currentMe // Trigger StateFlow emission
        } else {
            // Update Opponent Card Count
            val currentOpponents = _opponents.value.toMutableMap()
            val opponent = currentOpponents[event.playerId] ?: return

            val totalChange = event.changes.values.sum()
            // We create a new snapshot with updated count (Data classes are immutable usually)
            currentOpponents[event.playerId] = opponent.copy(
                resourceCount = opponent.resourceCount + totalChange
            )
            _opponents.value = currentOpponents
        }
    }

    private fun handleObjectBuilt(event: GameplayEvent.ObjectBuilt) {
        val board = _board.value ?: return

        // Update Board Visuals
        val def = buildingDefs.value.firstOrNull { it.id == event.buildingId } ?: return

        if (event.hexC != null) {
            board.placeVertexBuilding(def.id, event.playerId, event.hexA, event.hexB, event.hexC!!)
        } else {
            board.placeEdgeBuilding(def.id, event.playerId, event.hexA, event.hexB)
        }

        // Trigger board refresh
        _board.value = board

        // Update Victory Points (Simple Client-side tracking)
        // Ideally, server sends a "ScoreUpdated" event to be authoritative.
        if (event.playerId == _me.value?.id) {
            _me.value!!.victoryPoints += def.points
        } else {
            // opponent.victoryPoints += def.points
        }
    }

    // --- User Actions (Sending Intents) ---

    private fun sendIntent(intent: GameplayIntent) {
        viewModelScope.launch {
            repository.sendMessage(intent)
        }
    }

    fun onBuildRoadClicked(h1: HexCoord, h2: HexCoord) {
        sendIntent(GameplayIntent.Build(
            buildingId = "road",
            hexA = h1,
            hexB = h2,
            hexC = null
        ))
    }

    fun onBuildSettlementClicked(h1: HexCoord, h2: HexCoord, h3: HexCoord) {
        sendIntent(GameplayIntent.Build(
            buildingId = "settlement",
            hexA = h1,
            hexB = h2,
            hexC = h3
        ))
    }

    fun onEndTurnClicked() {
        sendIntent(GameplayIntent.EndTurn())
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}