package eric.bitria.hexonkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.ui.board.CatanBoardScene
import eric.bitria.hexonkmp.ui.components.hud.ActionButton
import eric.bitria.hexonkmp.ui.components.cards.BuildingCards
import eric.bitria.hexonkmp.ui.components.cards.DevCardVisuals
import eric.bitria.hexonkmp.ui.components.cards.DevelopmentCards
import eric.bitria.hexonkmp.ui.components.sheets.DiscardSheet
import eric.bitria.hexonkmp.ui.components.hud.GameHeader
import eric.bitria.hexonkmp.ui.components.hud.PlayerPanel
import eric.bitria.hexonkmp.ui.components.hud.PlayerVisuals
import eric.bitria.hexonkmp.ui.components.cards.ResourceCards
import eric.bitria.hexonkmp.ui.components.hud.RollBadge
import eric.bitria.hexonkmp.ui.components.sheets.TradeSheet
import eric.bitria.hexonkmp.ui.components.sheets.WinnerDialog
import eric.bitria.hexonkmp.ui.screens.game.BuildMode
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.screens.game.GameViewModel
import eric.bitria.hexonkmp.ui.theme.Spacing
import io.github.erkko68.filament.Engine
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(engine: Engine, viewModel: GameViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val opts by viewModel.buildOptions.collectAsStateWithLifecycle()
    val proposeDraft by viewModel.proposeDraft.collectAsStateWithLifecycle()
    val discardDraft by viewModel.discardDraft.collectAsStateWithLifecycle()

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
                    victoryPointsOf = { viewModel.victoryPoints(s, it) },
                    canBuyDevCard = opts.canBuyDevCard,
                    onBuyDevCard = viewModel::buyDevCard,
                    onPlayDevCard = viewModel::playDevCard,
                    discardRequired = viewModel.discardOwed(s),
                    discardSelected = discardDraft,
                    onCycleDiscard = viewModel::cycleDiscard,
                    onClearDiscard = viewModel::clearDiscardDraft,
                    onSubmitDiscard = viewModel::submitDiscard,
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
                    onReturnToMenu = viewModel::leaveGame,
                )
            }
            is GameUiState.Error -> ErrorContent(s.message, onRetry = viewModel::retryJoinGame)
        }

        if (state is GameUiState.Waiting) {
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
    ghostSettlements: List<Vertex>,
    ghostRoads: List<Edge>,
    ghostCities: List<Vertex>,
    robberTargets: List<Axial>,
    bankRatio: Int,
    victoryPointsOf: (PlayerId) -> Int,
    canBuyDevCard: Boolean,
    onBuyDevCard: () -> Unit,
    onPlayDevCard: (DevCard) -> Unit,
    discardRequired: Int,
    discardSelected: ResourceCount,
    onCycleDiscard: (Resource) -> Unit,
    onClearDiscard: () -> Unit,
    onSubmitDiscard: () -> Unit,
    onToggleSettlement: () -> Unit,
    onToggleRoad: () -> Unit,
    onToggleCity: () -> Unit,
    onPickVertex: (Vertex) -> Unit,
    onPickEdge: (Edge) -> Unit,
    onPickHex: (Axial) -> Unit,
    onBankTrade: (List<BankSwap>) -> Unit,
    proposeGive: ResourceCount,
    proposeReceive: ResourceCount,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClearPropose: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onEndTurn: () -> Unit,
    onReturnToMenu: () -> Unit,
) {
    var showTradeSheet by remember { mutableStateOf(false) }
    var confirmPlay by remember { mutableStateOf<DevCard?>(null) }

    val me = state.myPlayerId
    val players = state.state.players

    Box(Modifier.fillMaxSize()) {
        CatanBoardScene(
            state = state.state,
            engine = engine,
            modifier = Modifier.fillMaxSize(),
            me = me,
            ghostSettlements = ghostSettlements,
            ghostRoads = ghostRoads,
            ghostCities = ghostCities,
            robberTargets = robberTargets,
            onPickVertex = onPickVertex,
            onPickEdge = onPickEdge,
            onPickHex = onPickHex,
        )

        // --- Top bar + player panels (left edge) ---
        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            GameHeader(
                phaseLabel = phaseLabel(state.state.phase),
                turnLabel = PlayerVisuals.label(state.state.currentPlayer, players, me),
                timeLabel = "00:00",
                victoryPoints = victoryPointsOf(me),
                victoryGoal = state.state.config.rules.victoryPointsToWin,
                onLeave = onReturnToMenu,
            )
            Column(
                modifier = Modifier.padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                players.forEach { p ->
                    PlayerPanel(
                        color = PlayerVisuals.color(p, players),
                        label = PlayerVisuals.label(p, players, me),
                        resourceCount = state.state.resourceCounts[p] ?: state.state.handOf(p).total,
                        devCardCount = state.state.devCardCounts[p] ?: state.state.devCardCountOf(p),
                        victoryPoints = victoryPointsOf(p),
                        isCurrentTurn = p == state.state.currentPlayer,
                        present = p in state.state.present,
                    )
                }
            }
        }

        // --- Dice roll badge ---
        state.state.lastRoll?.let { roll ->
            RollBadge(roll, modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp))
        }

        // --- Robber prompt / transient notice ---
        val robberPrompt = (state.state.phase is GamePhase.Robber && state.isMyTurn)
        val notice = if (robberPrompt) "Move the robber — tap a tile" else state.notice
        notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (robberPrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 152.dp),
            )
        }

        // --- Build / buy / trade action bar ---
        val hasIncomingOffer = state.state.pendingTrades.any { it.proposer != me }
        val tradeBadge = if (state.isMyTurn) {
            state.state.pendingTrades.any { it.accepters.isNotEmpty() }
        } else {
            state.state.pendingTrades.any { it.proposer != me && me !in it.responses }
        }
        val canTrade = state.state.phase is GamePhase.Play && (state.isMyTurn || hasIncomingOffer)
        BuildingCards(
            canSettlement = canBuildSettlement,
            settlementSelected = state.buildMode == BuildMode.SETTLEMENT,
            canRoad = canBuildRoad,
            roadSelected = state.buildMode == BuildMode.ROAD,
            canCity = canBuildCity,
            citySelected = state.buildMode == BuildMode.CITY,
            canBuyDevCard = canBuyDevCard,
            canTrade = canTrade,
            tradeSelected = showTradeSheet,
            tradeBadge = tradeBadge,
            onSettlement = onToggleSettlement,
            onRoad = onToggleRoad,
            onCity = onToggleCity,
            onBuyDevCard = onBuyDevCard,
            onTrade = { showTradeSheet = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md),
        )

        if (showTradeSheet) {
            TradeSheet(
                ratio = bankRatio,
                hand = state.state.handOf(me),
                me = me,
                isMyTurn = state.isMyTurn,
                playerColor = { PlayerVisuals.color(it, players) },
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

        if (discardRequired > 0) {
            DiscardSheet(
                required = discardRequired,
                hand = state.state.handOf(me),
                selected = discardSelected,
                onCycle = onCycleDiscard,
                onClear = onClearDiscard,
                onSubmit = onSubmitDiscard,
            )
        }

        // --- Dev cards + resource cards, bottom-left ---
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val playable = state.state.devCards[me].orEmpty()
            val held = playable + state.state.boughtThisTurn[me].orEmpty()
            val canPlay = state.isMyTurn &&
                state.state.phase is GamePhase.Play &&
                !state.state.devCardPlayed
            // Currently only Knights are implemented; extend the set as other cards are wired up.
            val playableCards = if (canPlay) playable.filter { it == DevCard.KNIGHT }.toSet() else emptySet()
            DevelopmentCards(
                cards = held.groupingBy { it }.eachCount(),
                playable = playableCards,
                onPlay = { confirmPlay = it },
            )
            ResourceCards(hand = state.state.handOf(me))
        }

        // --- Confirm dialog before spending a dev card ---
        confirmPlay?.let { card ->
            AlertDialog(
                onDismissRequest = { confirmPlay = null },
                title = { Text("Play ${DevCardVisuals.label(card)}?") },
                text = { Text(devCardPrompt(card)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmPlay = null
                        onPlayDevCard(card)
                    }) { Text("Play") }
                },
                dismissButton = { TextButton(onClick = { confirmPlay = null }) { Text("Cancel") } },
            )
        }

        // --- End Turn button, bottom-right (Play phase only) ---
        if (state.state.phase is GamePhase.Play) {
            ActionButton(
                icon = Icons.Filled.SkipNext,
                label = "End turn",
                enabled = state.isMyTurn,
                onClick = onEndTurn,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
            )
        }

        // --- Winner overlay ---
        (state.state.phase as? GamePhase.Finished)?.let { finished ->
            WinnerDialog(
                color = PlayerVisuals.color(finished.winner, players),
                label = PlayerVisuals.label(finished.winner, players, me),
                youWon = finished.winner == me,
                onReturnToMenu = onReturnToMenu,
            )
        }
    }
}

private fun phaseLabel(phase: GamePhase): String = when (phase) {
    is GamePhase.Setup -> "Setup"
    GamePhase.Play -> "Play"
    is GamePhase.Discard -> "Discard"
    GamePhase.Robber -> "Robber"
    is GamePhase.Finished -> "Finished"
}

private fun devCardPrompt(card: DevCard): String = when (card) {
    DevCard.KNIGHT -> "Move the robber and steal a card. Counts toward Largest Army."
    DevCard.ROAD_BUILDING -> "Place two roads for free."
    DevCard.YEAR_OF_PLENTY -> "Take any two resources from the bank."
    DevCard.MONOPOLY -> "Name a resource; every other player gives you all of theirs."
    DevCard.VICTORY_POINT -> "A hidden victory point — it can't be played."
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
