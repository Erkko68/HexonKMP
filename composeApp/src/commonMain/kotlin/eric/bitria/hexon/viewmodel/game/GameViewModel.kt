package eric.bitria.hexon.viewmodel.game

import androidx.lifecycle.ViewModel
import eric.bitria.hexon.viewmodel.data.ItemCardData
import eric.bitria.hexon.viewmodel.data.Player
import eric.bitria.hexon.viewmodel.enums.GameUIState

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUIState.ROLLING)
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    private val _players = MutableStateFlow(listOf<Player>())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _resources = MutableStateFlow(listOf<ItemCardData>())
    val resources: StateFlow<List<ItemCardData>> = _resources.asStateFlow()

    private val _assets = MutableStateFlow(listOf<ItemCardData>())
    val assets: StateFlow<List<ItemCardData>> = _assets.asStateFlow()

    private val _progressCards = MutableStateFlow(listOf<ItemCardData>())
    val progressCards: StateFlow<List<ItemCardData>> = _progressCards.asStateFlow()

    private val _victoryPoints = MutableStateFlow(Pair(0,0))
    val victoryPoints: StateFlow<Pair<Int,Int>> = _victoryPoints.asStateFlow()

    fun setUIState(state: GameUIState) {
        _uiState.value = state
    }

    fun onTradeActionClick() {
        _uiState.value = GameUIState.TRADING
    }
}