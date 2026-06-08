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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import eric.bitria.hexonkmp.ui.theme.rememberSvgPainter
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
import eric.bitria.hexonkmp.ui.components.sheets.StealTargetSheet
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
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private enum class PortraitBottomTab {
    ACTIONS,
    INVENTORY
}

@Composable
fun PortraitGameLayout(
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
    onStealFrom: (PlayerId) -> Unit,
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
                        label = state.displayName(p),
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


        // Contextual notice text (rendered just above the action bar, below).
        val roadBuildingPhase = state.state.phase as? GamePhase.RoadBuilding
        val notice = when {
            roadBuildingPhase != null -> stringResource(Res.string.notice_place_free_roads, roadBuildingPhase.roadsLeft)
            opts.robberTargets.isNotEmpty() -> stringResource(Res.string.notice_move_robber)
            state.state.phase is GamePhase.ChooseStealTarget && state.isMyTurn -> stringResource(Res.string.notice_choose_steal)
            else -> state.notice
        }

        // --- Toggleable bottom panel (notice sits on top of it) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Notice above the actions; wraps to multiple lines on narrow screens.
            notice?.let {
                NoticeChip(text = it, modifier = Modifier.padding(horizontal = Spacing.lg))
            }
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
                                stringResource(Res.string.tab_actions),
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
                                stringResource(Res.string.tab_inventory),
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
                            label = stringResource(Res.string.build_settlement),
                            enabled = opts.canSettlement,
                            selected = state.buildMode == BuildMode.SETTLEMENT,
                            onClick = onToggleSettlement,
                        ) {
                            Icon(rememberSvgPainter("files/icons/svg/ic_settlement.svg"), null, Modifier.fillMaxSize(0.6f))
                        }
                        ActionCard(
                            label = stringResource(Res.string.build_road),
                            enabled = opts.canRoad,
                            selected = state.buildMode == BuildMode.ROAD,
                            onClick = onToggleRoad,
                        ) {
                            Icon(rememberSvgPainter("files/icons/svg/ic_road.svg"), null, Modifier.fillMaxSize(0.6f))
                        }
                        ActionCard(
                            label = stringResource(Res.string.build_city),
                            enabled = opts.canCity,
                            selected = state.buildMode == BuildMode.CITY,
                            onClick = onToggleCity,
                        ) {
                            Icon(rememberSvgPainter("files/icons/svg/ic_city.svg"), null, Modifier.fillMaxSize(0.6f))
                        }
                        ActionCard(
                            label = stringResource(Res.string.buy_dev_card),
                            enabled = opts.canBuyDevCard,
                            onClick = onBuyDevCard,
                        ) {
                            Icon(rememberSvgPainter("files/icons/svg/ic_dev_card.svg"), null, Modifier.fillMaxSize(0.6f))
                        }
                        ActionCard(
                            label = stringResource(Res.string.trade),
                            enabled = opts.canTrade,
                            selected = showTradeSheet,
                            badge = opts.tradeBadge,
                            onClick = { showTradeSheet = true },
                        ) {
                            Icon(Icons.Filled.SwapHoriz, null, Modifier.fillMaxSize(0.6f))
                        }
                        ActionCard(
                            label = stringResource(Res.string.end_turn),
                            enabled = opts.canEndTurn,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            onClick = onEndTurn,
                        ) {
                            Icon(Icons.Filled.SkipNext, null, Modifier.fillMaxSize(0.6f))
                        }
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
                            hasLongestRoad = state.state.longestRoad == me,
                            hasLargestArmy = state.state.largestArmy == me,
                        )
                        ResourceBar(hand = state.state.handOf(me))
                    }
                }
            }
        }

        if (showTradeSheet) {
            TradeSheet(
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

        // --- Steal target selection (robber on a multi-opponent tile, my turn) ---
        val chooseStealPhase = state.state.phase as? GamePhase.ChooseStealTarget
        if (chooseStealPhase != null && state.isMyTurn) {
            StealTargetSheet(
                victims = chooseStealPhase.victims,
                playerColor = { PlayerPalette.color(it, players) },
                playerLabel = { state.displayName(it) },
                cardCount = { state.state.resourceCounts[it] ?: state.state.handOf(it).total },
                onStealFrom = onStealFrom,
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
                label = state.displayName(finished.winner),
                youWon = finished.winner == me,
                onReturnToMenu = onReturnToMenu,
            )
        }

    }
}

@Composable
private fun phaseLabel(phase: GamePhase): String = stringResource(
    when (phase) {
        is GamePhase.Setup -> Res.string.phase_setup
        GamePhase.Play -> Res.string.phase_play
        is GamePhase.Discard -> Res.string.phase_discard
        GamePhase.Robber -> Res.string.phase_robber
        is GamePhase.ChooseStealTarget -> Res.string.phase_robber
        is GamePhase.RoadBuilding -> Res.string.phase_road_building
        is GamePhase.Finished -> Res.string.phase_finished
    }
)

