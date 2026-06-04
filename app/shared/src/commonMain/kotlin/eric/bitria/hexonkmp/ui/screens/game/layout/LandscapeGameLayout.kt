package eric.bitria.hexonkmp.ui.screens.game.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.ui.board.CatanBoardScene
import eric.bitria.hexonkmp.ui.components.cards.ActionBar
import eric.bitria.hexonkmp.ui.components.cards.ActionCard
import eric.bitria.hexonkmp.ui.components.cards.DevelopmentBar
import eric.bitria.hexonkmp.ui.components.cards.ResourceBar
import eric.bitria.hexonkmp.ui.components.hud.LandscapeGameHeader
import eric.bitria.hexonkmp.ui.components.hud.LandscapePlayerPanel
import eric.bitria.hexonkmp.ui.components.hud.NoticeChip
import eric.bitria.hexonkmp.ui.components.sheets.DiscardSheet
import eric.bitria.hexonkmp.ui.components.sheets.TradeSidePanel
import eric.bitria.hexonkmp.ui.components.sheets.WinnerDialog
import eric.bitria.hexonkmp.ui.components.sheets.devcards.KnightSheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.MonopolySheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.RoadBuildingSheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.YearOfPlentySheet
import eric.bitria.hexonkmp.ui.screens.game.BuildMode
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.theme.PlayerPalette
import eric.bitria.hexonkmp.ui.theme.Spacing
import io.github.erkko68.filament.Engine

@Composable
fun LandscapeGameLayout(
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
    var showTradeSheet by remember { mutableStateOf(false) }
    var confirmPlay by remember { mutableStateOf<DevCard?>(null) }

    val me = state.myPlayerId
    val players = state.state.players
    val opts = state.buildOptions

    Box(Modifier.fillMaxSize()) {
        // --- 1. Main game area (takes full size, background) ---
        CatanBoardScene(
            state = state.state,
            engine = engine,
            modifier = Modifier.fillMaxSize(),
            me = me,
            ghostSettlements = opts.ghostSettlements,
            ghostRoads = opts.ghostRoads,
            ghostCities = opts.ghostCities,
            robberTargets = opts.robberTargets,
            onPickVertex = onPickVertex,
            onPickEdge = onPickEdge,
            onPickHex = onPickHex,
        )

        // --- 2. Overlay Layer ---
        Column(modifier = Modifier.fillMaxSize()) {
            LandscapeGameHeader(
                phaseLabel = phaseLabel(state.state.phase),
                timeLabel = "00:00",
                lastRoll = state.state.lastRoll,
                victoryPoints = victoryPointsOf(me),
                victoryGoal = state.state.config.rules.victoryPointsToWin,
                onLeave = onReturnToMenu,
            )

            // Content below header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Player panels (left edge)
                Column(
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    players.sortedByDescending { victoryPointsOf(it) }.forEach { p ->
                        LandscapePlayerPanel(
                            color = PlayerPalette.color(p, players),
                            label = PlayerPalette.label(p, players, me),
                            resourceCount = state.state.resourceCounts[p] ?: state.state.handOf(p).total,
                            devCardCount = state.state.devCardCounts[p] ?: state.state.devCardCountOf(p),
                            victoryPoints = victoryPointsOf(p),
                            isCurrentTurn = p == state.state.currentPlayer,
                            present = p in state.state.present,
                            modifier = Modifier.width(200.dp),
                        )
                    }
                }

                // Contextual notice chip (top center)
                val roadBuildingPhase = state.state.phase as? GamePhase.RoadBuilding
                val notice = when {
                    roadBuildingPhase != null -> "Place ${roadBuildingPhase.roadsLeft} free road(s) — tap a spot"
                    opts.robberTargets.isNotEmpty() -> "Move the robber — tap a tile"
                    else -> state.notice
                }
                notice?.let {
                    NoticeChip(
                        text = it,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.sm),
                    )
                }

                // Dev cards + resource bar (bottom-left)
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    val held = (state.state.devCards[me].orEmpty() + state.state.boughtThisTurn[me].orEmpty())
                        .groupingBy { it }.eachCount()
                    DevelopmentBar(
                        cards = held,
                        playable = opts.playableDevCards,
                        onPlay = { confirmPlay = it },
                    )
                    ResourceBar(hand = state.state.handOf(me))
                }

                // Action bar (bottom center)
                ActionBar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md),
                ) {
                    ActionCard(
                        icon = Icons.Filled.Home,
                        label = "Settlement",
                        enabled = opts.canSettlement,
                        selected = state.buildMode == BuildMode.SETTLEMENT,
                        onClick = onToggleSettlement
                    )
                    ActionCard(
                        icon = Icons.Filled.AddRoad,
                        label = "Road",
                        enabled = opts.canRoad,
                        selected = state.buildMode == BuildMode.ROAD,
                        onClick = onToggleRoad
                    )
                    ActionCard(
                        icon = Icons.Filled.LocationCity,
                        label = "City",
                        enabled = opts.canCity,
                        selected = state.buildMode == BuildMode.CITY,
                        onClick = onToggleCity
                    )
                    ActionCard(
                        icon = Icons.Filled.Style,
                        label = "Buy dev card",
                        enabled = opts.canBuyDevCard,
                        onClick = onBuyDevCard
                    )
                    ActionCard(
                        icon = Icons.Filled.SwapHoriz,
                        label = "Trade",
                        enabled = opts.canTrade,
                        selected = showTradeSheet,
                        badge = opts.tradeBadge,
                        onClick = { showTradeSheet = !showTradeSheet }
                    )
                    ActionCard(
                        icon = Icons.Filled.SkipNext,
                        label = "End turn",
                        enabled = opts.canEndTurn,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = onEndTurn
                    )
                }

                // Slide-in right side pane overlay for trading (fits height below header perfectly)
                if (showTradeSheet) {
                    TradeSidePanel(
                        bankRates = opts.bankRates,
                        canBankTrade = opts.canBankTrade,
                        hand = state.state.handOf(me),
                        me = me,
                        isMyTurn = state.isMyTurn,
                        playerColor = { PlayerPalette.color(it, players) },
                        offers = state.state.pendingTrades,
                        give = state.tradeDraft.give,
                        receive = state.tradeDraft.receive,
                        onBankTrade = {
                            onBankTrade()
                            showTradeSheet = false
                        },
                        onCycleGive = onCycleGive,
                        onCycleReceive = onCycleReceive,
                        onClear = onClearTrade,
                        onSubmitPropose = onSubmitPropose,
                        onRespondTrade = onRespondTrade,
                        onFinalizeTrade = onFinalizeTrade,
                        onCancelTrade = onCancelTrade,
                        onDismiss = { showTradeSheet = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // --- Overlays not constrained by header layout ---
        if (discardRequired > 0) {
            DiscardSheet(
                required = discardRequired,
                hand = state.state.handOf(me),
                selected = state.discardDraft,
                onCycle = onCycleDiscard,
                onClear = onClearDiscard,
                onSubmit = onSubmitDiscard,
            )
        }

        // Dev card sheet (one per card type)
        when (confirmPlay) {
            DevCard.KNIGHT -> KnightSheet(
                onConfirm = { onPlayDevCard(DevCard.KNIGHT) },
                onDismiss = { confirmPlay = null },
            )
            DevCard.ROAD_BUILDING -> RoadBuildingSheet(
                onConfirm = { onPlayDevCard(DevCard.ROAD_BUILDING) },
                onDismiss = { confirmPlay = null },
            )
            DevCard.YEAR_OF_PLENTY -> YearOfPlentySheet(
                onSubmit = { resources -> confirmPlay = null; onPlayYearOfPlenty(resources) },
                onDismiss = { confirmPlay = null },
            )
            DevCard.MONOPOLY -> MonopolySheet(
                onSubmit = { resource -> confirmPlay = null; onPlayMonopoly(resource) },
                onDismiss = { confirmPlay = null },
            )
            else -> Unit
        }

        // Winner overlay
        (state.state.phase as? GamePhase.Finished)?.let { finished ->
            WinnerDialog(
                color = PlayerPalette.color(finished.winner, players),
                label = PlayerPalette.label(finished.winner, players, me),
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
    is GamePhase.RoadBuilding -> "Road Building"
    is GamePhase.Finished -> "Finished"
}

