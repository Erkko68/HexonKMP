package eric.bitria.hexon.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.ui.components.game.ItemCardData
import eric.bitria.hexon.ui.components.game.Player
import eric.bitria.hexon.viewmodel.enums.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameUIViewModel : ViewModel (){

    private val _uiState = MutableStateFlow(GameUIState.WAITING)
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    // Players Turn
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _resources = MutableStateFlow<List<ItemCardData>>(emptyList())
    val resources: StateFlow<List<ItemCardData>> = _resources.asStateFlow()

    private val _assets = MutableStateFlow<List<ItemCardData>>(emptyList())
    val assets: StateFlow<List<ItemCardData>> = _assets.asStateFlow()

    private val _victoryPoints = MutableStateFlow(Pair(0,0))
    val victoryPoints: StateFlow<Pair<Int,Int>> = _victoryPoints.asStateFlow()

    init {
        fetchPlayers()
        fetchResources()
        fetchAssets()
        fetchVictoryPoints()
    }

    fun setUIState(newState: GameUIState) {
        _uiState.value = newState
    }

    fun fetchPlayers() {
        viewModelScope.launch {
            try {
                val remotePlayers = listOf(
                    Player("Player 1", Color(0xFF81D4FA)),
                    Player("Player 2", Color(0xFFE57373)),
                    Player("Player 3", Color(0xFF81C784)),
                )
                _players.value = remotePlayers
            } catch (e: Exception) {
                // ERROR
            }
        }
    }

    fun fetchResources(){
        viewModelScope.launch {
            try {
                val playerResources = listOf(
                    ItemCardData("4", Icons.Filled.LocalFlorist, "Wool", Color(0xFFBA68C8), Color(0xFFCE93D8)),
                    ItemCardData("2", Icons.Filled.Public, "Ore", Color(0xFFFFF59D), Color(0xFFFFF59D)),
                    ItemCardData("5", Icons.Filled.Park, "Lumber", Color(0xFFFFB74D), Color(0xFFFFCC80)),
                    ItemCardData("1", Icons.Filled.Terrain, "Brick", Color(0xFFB0BEC5), Color(0xFFCFD8DC)),
                    ItemCardData("3", Icons.Filled.Agriculture, "Grain", Color(0xFFFFEB3B), Color(0xFFFFEB3B))
                )
                _resources.value = playerResources
            } catch (e: Exception) {
                // ERROR
            }
        }
    }

    fun fetchAssets(){
        viewModelScope.launch {
            try {
                val playerAssets = listOf(
                    ItemCardData("1", Icons.Filled.Straighten, "Wood", Color(0xFF8D6E63), Color(0xFFA1887F)),
                    ItemCardData("2", Icons.Filled.Home, "Brick", Color(0xFFE57373), Color(0xFFEF9A9A)),
                    ItemCardData("3", Icons.Filled.Castle, "Stone", Color(0xFF757575), Color(0xFFBDBDBD)),
                    ItemCardData("4", Icons.Filled.AutoAwesome, "Wool", Color(0xFFBA68C8), Color(0xFFCE93D8))
                )

                _assets.value = playerAssets
            } catch (e: Exception) {
                // ERROR
            }
        }
    }

    fun fetchVictoryPoints(){
        viewModelScope.launch {
            try {
                val playerVictoryPoints = Pair(7,10)
                _victoryPoints.value = playerVictoryPoints
            } catch (e: Exception) {
                // ERROR
            }
        }
    }

}