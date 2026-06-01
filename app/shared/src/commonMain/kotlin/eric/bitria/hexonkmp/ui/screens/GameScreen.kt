package eric.bitria.hexonkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.Placement
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import eric.bitria.hexonkmp.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(viewModel: GameViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is GameUiState.Idle -> IdleContent(onFindGame = viewModel::joinGame)
            is GameUiState.Connecting -> ConnectingContent()
            is GameUiState.Waiting -> WaitingContent(s)
            is GameUiState.InGame -> InGameContent(
                state = s,
                legalSettlements = viewModel.legalSettlements(s),
                legalRoads = viewModel.legalRoads(s),
                onPlaceSettlement = viewModel::placeSettlement,
                onPlaceRoad = viewModel::placeRoad,
                onEndTurn = viewModel::endTurn,
            )
            is GameUiState.Error -> ErrorContent(s.message, onRetry = viewModel::retryJoinGame)
        }

        if (state is GameUiState.Waiting || state is GameUiState.InGame) {
            TextButton(
                onClick = viewModel::leaveGame,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm),
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.height(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Leave")
            }
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
    legalSettlements: List<Vertex>,
    legalRoads: List<Edge>,
    onPlaceSettlement: (Vertex) -> Unit,
    onPlaceRoad: (Edge) -> Unit,
    onEndTurn: () -> Unit,
) {
    val setup = state.state.phase as? GamePhase.Setup
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (setup != null) "Setup — place your starting pieces" else "Game board goes here",
            style = MaterialTheme.typography.titleMedium,
        )
        if (setup == null) Text("Turn ${state.state.turn}", style = MaterialTheme.typography.bodyMedium)
        state.state.lastRoll?.let { roll ->
            Text("🎲 Last roll: $roll", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            if (state.isMyTurn) "Your turn" else "Waiting for ${state.state.currentPlayer.value}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (state.isMyTurn) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ResourceHand(state.state.handOf(state.myPlayerId))

        when {
            // Setup: offer the legal placements as a picker (no board yet).
            setup != null && state.isMyTurn && setup.awaiting == Placement.SETTLEMENT ->
                PlacementPicker("Choose a settlement spot", legalSettlements.size) { i ->
                    onPlaceSettlement(legalSettlements[i])
                }
            setup != null && state.isMyTurn && setup.awaiting == Placement.ROAD ->
                PlacementPicker("Choose a road", legalRoads.size) { i ->
                    onPlaceRoad(legalRoads[i])
                }
            // Play: normal end-of-turn.
            setup == null ->
                Button(onClick = onEndTurn, enabled = state.isMyTurn) { Text("End Turn") }
        }

        state.notice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

// A temporary, board-free way to place pieces during setup: a scrollable list of
// the legal options as numbered buttons. Replaced by tapping the board later.
@Composable
private fun PlacementPicker(label: String, count: Int, onPick: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Text("$count legal options", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count) { i ->
            Button(onClick = { onPick(i) }) { Text("Option ${i + 1}") }
        }
    }
}

@Composable
private fun ResourceHand(hand: ResourceCount) {
    if (hand.isEmpty) {
        Text("No resources yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val text = Resource.entries
        .filter { hand[it] > 0 }
        .joinToString("   ") { "${it.label} ${hand[it]}" }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

// Short display label per resource for the simple text-based hand.
private val Resource.label: String
    get() = when (this) {
        Resource.BRICK -> "🧱"
        Resource.LUMBER -> "🌲"
        Resource.WOOL -> "🐑"
        Resource.GRAIN -> "🌾"
        Resource.ORE -> "⛰️"
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
