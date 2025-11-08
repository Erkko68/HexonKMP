package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import eric.bitria.hexon.viewmodel.enums.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameUIViewModel : ViewModel (){

    private val _uiState = MutableStateFlow(GameUIState.WAITING)
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    fun setUIState(newState: GameUIState) {
        _uiState.value = newState
    }
}