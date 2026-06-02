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
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.ui.board.CatanBoardScene
import eric.bitria.hexonkmp.ui.components.TradeSheet
import eric.bitria.hexonkmp.ui.components.BuildCard
import eric.bitria.hexonkmp.ui.components.PlayerCard
import eric.bitria.hexonkmp.ui.components.ResourceCards
import eric.bitria.hexonkmp.ui.components.RollBadge
import eric.bitria.hexonkmp.ui.components.TurnIndicator
import eric.bitria.hexonkmp.ui.screens.game.BuildMode
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import eric.bitria.hexonkmp.ui.theme.Spacing
import io.github.erkko68.filament.Engine
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(engine: Engine, viewModel: GameViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Derived placement options come from the ViewModel as their own flow, so the
    // expensive legal-move scans run once per state change, not in composition.
    val opts by viewModel.buildOptions.collectAsStateWithLifecycle()
    // The propose-trade draft is VM-owned too (its own flow), so building an offer
    // doesn't disturb the game state or re-run the legal-move scans.
    val proposeDraft by viewModel.proposeDraft.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is GameUiState.Idle -> IdleContent(onFindGame = viewModel::joinGame)
            is GameUiState.Connecting -> ConnectingContent()
            is GameUiState.Waiting -> WaitingContent(s)
            is GameUiState.InGame -> {
                InGameContent(
                    state = s,
                    engine = engine,
                    canBuildSettlement = opts.canSettlement,
                    canBuildRoad = opts.canRoad,
                    canBuildCity = opts.canCity,
                    ghostSettlements = opts.ghostSettlements,
                    ghostRoads = opts.ghostRoads,
                    ghostCities = opts.ghostCities,
                    robberTargets = opts.robberTargets,
                    bankRatio = viewModel.bankTradeRatio(s),
                    myVictoryPoints = viewModel.victoryPoints(s, s.myPlayerId),
                    onToggleSettlement = { viewModel.toggleBuildMode(BuildMode.SETTLEMENT) },
                    onToggleRoad = { viewModel.toggleBuildMode(BuildMode.ROAD) },
                    onToggleCity = { viewModel.toggleBuildMode(BuildMode.CITY) },
                    onPickVertex = viewModel::pickVertex,
                    onPickEdge = viewModel::pickEdge,
                    onPickHex = viewModel::pickHex,
                    onBankTrade = viewModel::bankTrade,
                    proposeGive = proposeDraft.give,
                    proposeReceive = proposeDraft.receive,
                    onCycleGive = viewModel::cycleGive,
                    onCycleReceive = viewModel::cycleReceive,
                    onClearPropose = viewModel::clearProposeDraft,
                    onSubmitPropose = viewModel::submitProposal,
                    onRespondTrade = viewModel::respondTrade,
                    onFinalizeTrade = viewModel::finalizeTrade,
                    onCancelTrade = viewModel::cancelTrade,
                    onEndTurn = viewModel::endTurn,
                )
            }
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
    canBuildCity: Boolean,
    ghostSettlements: List<eric.bitria.hexonkmp.core.game.model.board.Vertex>,
    ghostRoads: List<eric.bitria.hexonkmp.core.game.model.board.Edge>,
    ghostCities: List<eric.bitria.hexonkmp.core.game.model.board.Vertex>,
    robberTargets: List<eric.bitria.hexonkmp.core.game.model.board.Axial>,
    bankRatio: Int,
    myVictoryPoints: Int,
    onToggleSettlement: () -> Unit,
    onToggleRoad: () -> Unit,
    onToggleCity: () -> Unit,
    onPickVertex: (eric.bitria.hexonkmp.core.game.model.board.Vertex) -> Unit,
    onPickEdge: (eric.bitria.hexonkmp.core.game.model.board.Edge) -> Unit,
    onPickHex: (eric.bitria.hexonkmp.core.game.model.board.Axial) -> Unit,
    onBankTrade: (List<eric.bitria.hexonkmp.core.game.action.BankSwap>) -> Unit,
    proposeGive: eric.bitria.hexonkmp.core.game.model.ResourceCount,
    proposeReceive: eric.bitria.hexonkmp.core.game.model.ResourceCount,
    onCycleGive: (eric.bitria.hexonkmp.core.game.model.board.Resource) -> Unit,
    onCycleReceive: (eric.bitria.hexonkmp.core.game.model.board.Resource) -> Unit,
    onClearPropose: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, eric.bitria.hexonkmp.core.game.model.PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onEndTurn: () -> Unit,
) {
    val setup = state.state.phase as? GamePhase.Setup
    var showTradeSheet by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        // The 3D board fills the screen; HUD + cards overlay on top.
        CatanBoardScene(
            state = state.state,
            engine = engine,
            modifier = Modifier.fillMaxSize(),
            me = state.myPlayerId,
            ghostSettlements = ghostSettlements,
            ghostRoads = ghostRoads,
            ghostCities = ghostCities,
            robberTargets = robberTargets,
            onPickVertex = onPickVertex,
            onPickEdge = onPickEdge,
            onPickHex = onPickHex,
        )

        // --- Turn indicator + player chips, top-left ---
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Phase card + your victory-point count beside it.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TurnIndicator(
                    phaseLabel = if (setup != null) "Setup" else "Turn ${state.state.turn}",
                    statusLabel = if (state.isMyTurn) "Your turn" else "Waiting for opponent",
                    isMyTurn = state.isMyTurn,
                )
                VictoryPointBadge(points = myVictoryPoints)
            }
            // One chip per player in seat order: outlined for the current player,
            // dimmed/struck-through for anyone who has left.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                state.state.players.forEach { p ->
                    PlayerCard(
                        player = p,
                        players = state.state.players,
                        me = state.myPlayerId,
                        current = p == state.state.currentPlayer,
                        present = p in state.state.present,
                        size = 40,
                    )
                }
            }
        }

        // --- Dice roll badge, top-center (prominent) ---
        state.state.lastRoll?.let { roll ->
            RollBadge(roll, modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.md))
        }

        // --- Robber prompt / transient notice, just below the roll badge ---
        val robberPrompt = (state.state.phase is GamePhase.Robber && state.isMyTurn)
        val notice = if (robberPrompt) "Move the robber — tap a tile" else state.notice
        notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (robberPrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
            )
        }

        // --- Build cards (icon-only), bottom-center. Tapping arms a build mode
        // (highlighted) which shows ghost markers on the board; tap to place. ---
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BuildCard(
                icon = Icons.Filled.Home,
                label = "Settlement",
                enabled = canBuildSettlement,
                selected = state.buildMode == BuildMode.SETTLEMENT,
                onClick = onToggleSettlement,
            )
            BuildCard(
                icon = Icons.Filled.AddRoad,
                label = "Road",
                enabled = canBuildRoad,
                selected = state.buildMode == BuildMode.ROAD,
                onClick = onToggleRoad,
            )
            BuildCard(
                icon = Icons.Filled.LocationCity,
                label = "City",
                enabled = canBuildCity,
                selected = state.buildMode == BuildMode.CITY,
                onClick = onToggleCity,
            )
            // Trade is available during your Play turn (not setup) to propose/bank-
            // trade, or whenever another player has an offer waiting for your reply.
            val me = state.myPlayerId
            val hasIncomingOffer = state.state.pendingTrades.any { it.proposer != me }
            // Notification dot: proposer sees it when someone accepts one of their
            // offers; opponents see it for an offer they haven't responded to yet.
            val tradeBadge = if (state.isMyTurn) {
                state.state.pendingTrades.any { it.accepters.isNotEmpty() }
            } else {
                state.state.pendingTrades.any { it.proposer != me && me !in it.responses }
            }
            BuildCard(
                icon = Icons.Filled.SwapHoriz,
                label = "Trade",
                enabled = (state.isMyTurn && setup == null) || hasIncomingOffer,
                selected = showTradeSheet,
                badge = tradeBadge,
                onClick = { showTradeSheet = true },
            )
        }

        if (showTradeSheet) {
            TradeSheet(
                ratio = bankRatio,
                hand = state.state.handOf(state.myPlayerId),
                me = state.myPlayerId,
                isMyTurn = state.isMyTurn,
                players = state.state.players,
                offers = state.state.pendingTrades,
                proposeGive = proposeGive,
                proposeReceive = proposeReceive,
                onBankTrade = { swaps ->
                    onBankTrade(swaps)
                    showTradeSheet = false
                },
                onCycleGive = onCycleGive,
                onCycleReceive = onCycleReceive,
                onClearPropose = onClearPropose,
                onSubmitPropose = onSubmitPropose,
                onRespondTrade = onRespondTrade,
                onFinalizeTrade = onFinalizeTrade,
                onCancelTrade = onCancelTrade,
                onDismiss = { showTradeSheet = false },
            )
        }

        // --- Resource cards, bottom-left ---
        ResourceCards(
            hand = state.state.handOf(state.myPlayerId),
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md),
        )

        // --- End Turn, bottom-right (Play phase only — hidden during setup and
        // while a robber move is owed). ---
        if (state.state.phase is GamePhase.Play) {
            Button(
                onClick = onEndTurn,
                enabled = state.isMyTurn,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
            ) { Text("End Turn") }
        }
    }
}

// Compact victory-point count for the top-left HUD, beside the phase card.
@Composable
private fun VictoryPointBadge(points: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(Icons.Filled.Star, contentDescription = "Victory points", modifier = Modifier.height(18.dp))
            Text("$points", style = MaterialTheme.typography.titleMedium)
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
