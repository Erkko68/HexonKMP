package eric.bitria.hexonkmp.ui.screens.game.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.action.BankSwap
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
import eric.bitria.hexonkmp.ui.components.hud.PortraitPlayerPanel
import eric.bitria.hexonkmp.ui.components.hud.NoticeChip
import eric.bitria.hexonkmp.ui.components.hud.PortraitGameHeader
import eric.bitria.hexonkmp.ui.components.sheets.DiscardSheet
import eric.bitria.hexonkmp.ui.components.sheets.TradeSheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.KnightSheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.MonopolySheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.RoadBuildingSheet
import eric.bitria.hexonkmp.ui.components.sheets.devcards.YearOfPlentySheet
import eric.bitria.hexonkmp.ui.components.sheets.WinnerDialog
import eric.bitria.hexonkmp.ui.screens.game.BuildMode
import eric.bitria.hexonkmp.ui.screens.game.GameUiState
import eric.bitria.hexonkmp.ui.theme.PlayerPalette
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import io.github.erkko68.filament.Engine

private enum class PortraitBottomTab {
    ACTIONS,
    INVENTORY
}

@Composable
fun PortraitGameLayout(
    state: GameUiState.InGame,
    engine: Engine,
    bankRatio: Int,
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
    onBankTrade: (List<BankSwap>) -> Unit,
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
    var activeTab by remember { mutableStateOf(PortraitBottomTab.ACTIONS) }
    var statsExpanded by remember { mutableStateOf(false) }

    val me = state.myPlayerId
    val players = state.state.players
    val opts = state.buildOptions

    Box(Modifier.fillMaxSize()) {
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

        // --- Top bar + player panels (left edge) ---
        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            PortraitGameHeader(
                phaseLabel = phaseLabel(state.state.phase),
                timeLabel = "00:00",
                lastRoll = state.state.lastRoll,
                onLeave = onReturnToMenu,
            )
            Column(
                modifier = Modifier.padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                players.sortedByDescending { victoryPointsOf(it) }.forEach { p ->
                    PortraitPlayerPanel(
                        color = PlayerPalette.color(p, players),
                        label = PlayerPalette.label(p, players, me),
                        resourceCount = state.state.resourceCounts[p] ?: state.state.handOf(p).total,
                        devCardCount = state.state.devCardCounts[p] ?: state.state.devCardCountOf(p),
                        victoryPoints = victoryPointsOf(p),
                        isCurrentTurn = p == state.state.currentPlayer,
                        present = p in state.state.present,
                        isExpanded = statsExpanded,
                        onClick = { statsExpanded = !statsExpanded }
                    )
                }
            }
        }


        // --- Contextual notice chip ---
        val roadBuildingPhase = state.state.phase as? GamePhase.RoadBuilding
        val notice = when {
            roadBuildingPhase != null -> "Place ${roadBuildingPhase.roadsLeft} free road(s) — tap a spot"
            opts.robberTargets.isNotEmpty() -> "Move the robber — tap a tile"
            else -> state.notice
        }
        notice?.let {
            NoticeChip(
                text = it,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 152.dp),
            )
        }

        // --- Toggleable bottom panel ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Pill toggle button
            Surface(
                shape = Shapes.pill,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Actions tab
                    Surface(
                        onClick = { activeTab = PortraitBottomTab.ACTIONS },
                        shape = Shapes.pill,
                        color = if (activeTab == PortraitBottomTab.ACTIONS) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == PortraitBottomTab.ACTIONS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        ) {
                            Text(
                                "Actions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Inventory tab
                    Surface(
                        onClick = { activeTab = PortraitBottomTab.INVENTORY },
                        shape = Shapes.pill,
                        color = if (activeTab == PortraitBottomTab.INVENTORY) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == PortraitBottomTab.INVENTORY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        ) {
                            Text(
                                "Inventory",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                PortraitBottomTab.ACTIONS -> {
                    ActionBar {
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
                            onClick = { showTradeSheet = true }
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
                }
                PortraitBottomTab.INVENTORY -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
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
                }
            }
        }

        if (showTradeSheet) {
            TradeSheet(
                ratio = bankRatio,
                hand = state.state.handOf(me),
                me = me,
                isMyTurn = state.isMyTurn,
                playerColor = { PlayerPalette.color(it, players) },
                offers = state.state.pendingTrades,
                proposeGive = state.proposeDraft.give,
                proposeReceive = state.proposeDraft.receive,
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
                selected = state.discardDraft,
                onCycle = onCycleDiscard,
                onClear = onClearDiscard,
                onSubmit = onSubmitDiscard,
            )
        }

        // --- Dev card sheet (one per card type) ---
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

        // --- Winner overlay ---
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

