package eric.bitria.hexonkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.ui.screens.game.BuildMode
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import eric.bitria.hexonkmp.ui.screens.game.layout.LandscapeGameLayout
import eric.bitria.hexonkmp.ui.screens.game.layout.PortraitGameLayout
import eric.bitria.hexonkmp.ui.theme.Spacing
import io.github.erkko68.filament.Engine
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(engine: Engine, viewModel: GameViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is GameUiState.Idle -> IdleContent(onFindGame = viewModel::joinGame)
            is GameUiState.Connecting -> ConnectingContent()
            is GameUiState.Waiting -> WaitingContent(s)
            is GameUiState.InGame -> {
                InGameContent(
                    state = s,
                    engine = engine,
                    victoryPointsOf = { viewModel.victoryPoints(s, it) },
                    onBuyDevCard = viewModel::buyDevCard,
                    onPlayDevCard = viewModel::playDevCard,
                    onPlayYearOfPlenty = viewModel::playYearOfPlenty,
                    onPlayMonopoly = viewModel::playMonopoly,
                    discardRequired = viewModel.discardOwed(s),
                    onCycleDiscard = viewModel::cycleDiscard,
                    onClearDiscard = viewModel::clearDiscardDraft,
                    onSubmitDiscard = viewModel::submitDiscard,
                    onToggleSettlement = { viewModel.toggleBuildMode(BuildMode.SETTLEMENT) },
                    onToggleRoad = { viewModel.toggleBuildMode(BuildMode.ROAD) },
                    onToggleCity = { viewModel.toggleBuildMode(BuildMode.CITY) },
                    onPickVertex = viewModel::pickVertex,
                    onPickEdge = viewModel::pickEdge,
                    onPickHex = viewModel::pickHex,
                    onBankTrade = viewModel::submitBankTrade,
                    onCycleGive = viewModel::cycleGive,
                    onCycleReceive = viewModel::cycleReceive,
                    onClearTrade = viewModel::clearTradeDraft,
                    onSubmitPropose = viewModel::submitProposal,
                    onRespondTrade = viewModel::respondTrade,
                    onFinalizeTrade = viewModel::finalizeTrade,
                    onCancelTrade = viewModel::cancelTrade,
                    onEndTurn = viewModel::endTurn,
                    onReturnToMenu = viewModel::leaveGame,
                )
            }
            is GameUiState.Error -> ErrorContent(s.message, onRetry = viewModel::retryJoinGame)
        }
    }
}

@Composable
private fun IdleContent(onFindGame: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hexon", style = MaterialTheme.typography.displayMedium)
        Text("Ready to play?", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onFindGame) { Text("Find Game") }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text("Connecting…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WaitingContent(state: GameUiState.Waiting) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text("Waiting for players…", style = MaterialTheme.typography.bodyLarge)
        Text(
            "${state.connected} / ${state.needed}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InGameContent(
    state: GameUiState.InGame,
    engine: Engine,
    victoryPointsOf: (PlayerId) -> Int,
    onBuyDevCard: () -> Unit,
    onPlayDevCard: (DevCard) -> Unit,
    onPlayYearOfPlenty: (ResourceCount) -> Unit,
    onPlayMonopoly: (Resource) -> Unit,
    discardRequired: Int,
    onCycleDiscard: (Resource) -> Unit,
    onClearDiscard: () -> Unit,
    onSubmitDiscard: () -> Unit,
    onToggleSettlement: () -> Unit,
    onToggleRoad: () -> Unit,
    onToggleCity: () -> Unit,
    onPickVertex: (Vertex) -> Unit,
    onPickEdge: (Edge) -> Unit,
    onPickHex: (Axial) -> Unit,
    onBankTrade: () -> Unit,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClearTrade: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onEndTurn: () -> Unit,
    onReturnToMenu: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            LandscapeGameLayout(
                state = state,
                engine = engine,
                victoryPointsOf = victoryPointsOf,
                onBuyDevCard = onBuyDevCard,
                onPlayDevCard = onPlayDevCard,
                onPlayYearOfPlenty = onPlayYearOfPlenty,
                onPlayMonopoly = onPlayMonopoly,
                discardRequired = discardRequired,
                onCycleDiscard = onCycleDiscard,
                onClearDiscard = onClearDiscard,
                onSubmitDiscard = onSubmitDiscard,
                onToggleSettlement = onToggleSettlement,
                onToggleRoad = onToggleRoad,
                onToggleCity = onToggleCity,
                onPickVertex = onPickVertex,
                onPickEdge = onPickEdge,
                onPickHex = onPickHex,
                onBankTrade = onBankTrade,
                onCycleGive = onCycleGive,
                onCycleReceive = onCycleReceive,
                onClearTrade = onClearTrade,
                onSubmitPropose = onSubmitPropose,
                onRespondTrade = onRespondTrade,
                onFinalizeTrade = onFinalizeTrade,
                onCancelTrade = onCancelTrade,
                onEndTurn = onEndTurn,
                onReturnToMenu = onReturnToMenu,
            )
        } else {
            PortraitGameLayout(
                state = state,
                engine = engine,
                victoryPointsOf = victoryPointsOf,
                onBuyDevCard = onBuyDevCard,
                onPlayDevCard = onPlayDevCard,
                onPlayYearOfPlenty = onPlayYearOfPlenty,
                onPlayMonopoly = onPlayMonopoly,
                discardRequired = discardRequired,
                onCycleDiscard = onCycleDiscard,
                onClearDiscard = onClearDiscard,
                onSubmitDiscard = onSubmitDiscard,
                onToggleSettlement = onToggleSettlement,
                onToggleRoad = onToggleRoad,
                onToggleCity = onToggleCity,
                onPickVertex = onPickVertex,
                onPickEdge = onPickEdge,
                onPickHex = onPickHex,
                onBankTrade = onBankTrade,
                onCycleGive = onCycleGive,
                onCycleReceive = onCycleReceive,
                onClearTrade = onClearTrade,
                onSubmitPropose = onSubmitPropose,
                onRespondTrade = onRespondTrade,
                onFinalizeTrade = onFinalizeTrade,
                onCancelTrade = onCancelTrade,
                onEndTurn = onEndTurn,
                onReturnToMenu = onReturnToMenu,
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
