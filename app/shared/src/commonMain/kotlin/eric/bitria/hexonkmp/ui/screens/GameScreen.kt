package eric.bitria.hexonkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import eric.bitria.hexonkmp.core.game.config.Buildable
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.board.CatanBoardScene
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
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
            is GameUiState.InGame -> InGameContent(
                state = s,
                engine = engine,
                canBuildSettlement = viewModel.canBuild(s, Buildable.SETTLEMENT),
                canBuildRoad = viewModel.canBuild(s, Buildable.ROAD),
                onBuildSettlement = viewModel::buildSettlement,
                onBuildRoad = viewModel::buildRoad,
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
    engine: Engine,
    canBuildSettlement: Boolean,
    canBuildRoad: Boolean,
    onBuildSettlement: () -> Unit,
    onBuildRoad: () -> Unit,
    onEndTurn: () -> Unit,
) {
    val setup = state.state.phase as? GamePhase.Setup
    Box(Modifier.fillMaxSize()) {
        // The 3D board fills the screen; HUD + cards overlay on top.
        CatanBoardScene(state.state, engine = engine, modifier = Modifier.fillMaxSize())

        // --- Status HUD, top-center ---
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                if (setup != null) "Setup" else "Turn ${state.state.turn}",
                style = MaterialTheme.typography.titleSmall,
            )
            state.state.lastRoll?.let { roll -> Text("🎲 $roll", style = MaterialTheme.typography.bodyMedium) }
            Text(
                if (state.isMyTurn) "Your turn" else "Waiting for ${state.state.currentPlayer.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isMyTurn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.notice?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        // --- Build cards, top area below the HUD ---
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BuildCard("🏠", "Settlement", enabled = canBuildSettlement, onClick = onBuildSettlement)
            BuildCard("🛣️", "Road", enabled = canBuildRoad, onClick = onBuildRoad)
        }

        // --- Resource cards, bottom-left ---
        ResourceCards(
            hand = state.state.handOf(state.myPlayerId),
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md),
        )

        // --- End Turn, bottom-right (Play phase only) ---
        if (setup == null) {
            Button(
                onClick = onEndTurn,
                enabled = state.isMyTurn,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
            ) { Text("End Turn") }
        }
    }
}

// A small build action presented as a card; dimmed and non-clickable when the
// player can't currently build it (not their turn / not enough resources).
@Composable
private fun BuildCard(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// The player's resource hand as a row of small cards (one per resource type that
// they hold), bottom-left.
@Composable
private fun ResourceCards(hand: ResourceCount, modifier: Modifier = Modifier) {
    val held = Resource.entries.filter { hand[it] > 0 }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (held.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "No resources",
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            held.forEach { res ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(res.label, style = MaterialTheme.typography.titleMedium)
                        Text("${hand[res]}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// Short display label per resource for the resource cards.
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
