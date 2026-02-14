package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.times
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.ui.components.game.IconActionButton
import eric.bitria.hexon.ui.components.game.OptionsButton
import eric.bitria.hexon.ui.components.game.PlayerTurnBar
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.ui.components.game.assets.BuildingRow
import eric.bitria.hexon.ui.components.game.assets.PlayerResourceRow
import eric.bitria.hexon.ui.components.game.assets.ResourceRow
import eric.bitria.hexon.ui.components.game.assets.TradeResourceRow
import eric.bitria.hexon.ui.components.game.trade.TradeRequest
import eric.bitria.hexon.viewmodel.game.GameViewModel
import kotlin.math.max

@Composable
fun GameUI(
    onExitClicked: () -> Unit,
    viewModel: GameViewModel,
) {
    val players by viewModel.opponents.collectAsState()
    val maxVictoryPoints by viewModel.victoryPoints.collectAsState()
    val resources by viewModel.resourcesDef.collectAsState()
    val buildings by viewModel.buildingsDef.collectAsState()
    val me by viewModel.me.collectAsState()

    val offeredResources by viewModel.offeredResources.collectAsState()
    val requestedResources by viewModel.requestedResources.collectAsState()

    val phase by viewModel.turnPhase.collectAsState()
    val activePlayerId by viewModel.activePlayerId.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val canSendBankExchange by viewModel.canSendBankExchangeBool.collectAsState()
    val canSendPlayerTrade by viewModel.canSendPlayerTradeBool.collectAsState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isLandscape = maxWidth > maxHeight

        // Use smaller dimension for more consistent padding
        val baseDim = if (isLandscape) maxHeight else maxWidth

        val outerPaddingH = baseDim * 0.03f
        val outerPaddingV = baseDim * 0.03f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = outerPaddingH, vertical = outerPaddingV)
        ) {

            // ================= TOP SECTION =================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isLandscape) 0.3f else 0.15f)
            ) {
                val sectionSpacing = maxHeight * 0.08f

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {

                    // ---- Top Bar ----
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerTurnBar(
                                activePlayerId = activePlayerId,
                                me = me,
                                opponents = players,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            OptionsButton(
                                onExitClicked = {
                                    onExitClicked()
                                    viewModel.onExitGame()
                                },
                                onAboutClicked = {},
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }

                    // ---- Victory Points ----
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.3f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VictoryPointsIndicator(
                                victoryPoints = Pair(
                                    me?.victoryPoints ?: 0,
                                    maxVictoryPoints
                                ),
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }

                    // ---- Trade Requests ----
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.3f)
                    ) {
                        if (trades.isNotEmpty()) {
                            val tradeHeight = maxHeight / trades.size
                            val width = maxWidth
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                trades.forEach { (playerId, tradeOffer) ->
                                    val playerColor = players[playerId]?.color ?: me?.color ?: "#000000"
                                    TradeRequest(
                                        playerId = playerId,
                                        playerColor = playerColor,
                                        tradeOffer = tradeOffer,
                                        onAccept = { viewModel.sendTradeResponse(playerId, true) },
                                        onDecline = { viewModel.sendTradeResponse(playerId, false) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(tradeHeight)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(if (isLandscape) 0.2f else 0.7f))

            // ================= BOTTOM SECTION =================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isLandscape) 0.55f else 0.25f)
            ) {

                val rowSpacing = maxHeight * 0.025f
                val columnSpacing = maxWidth * 0.03f
                val rowHeight = (maxHeight - rowSpacing * 3f) / 4f

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    verticalAlignment = Alignment.Bottom   // 👈 anchor to bottom
                ) {

                    // ---------- LEFT COLUMN ----------
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(
                                rowSpacing,
                                alignment = Alignment.Bottom
                            )
                        ) {

                            if (phase == TurnPhase.TRADE) {

                                ResourceRow(
                                    resources = resources,
                                    onClick = {
                                        viewModel.onRequestedResourceSelected(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(rowHeight)
                                )

                                TradeResourceRow(
                                    selected = requestedResources,
                                    onClick = {
                                        viewModel.onRequestedResourceDeselected(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(rowHeight)
                                )

                                TradeResourceRow(
                                    selected = offeredResources,
                                    onClick = {
                                        viewModel.onOfferedResourceDeselected(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(rowHeight)
                                )

                            } else {

                                BuildingRow(
                                    buildings = buildings,
                                    onClick = {
                                        viewModel.showAvailableBuildingPositions(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(rowHeight)
                                )
                            }

                            PlayerResourceRow(
                                me = me,
                                selected = offeredResources,
                                onClick = {
                                    viewModel.onOfferedResourceSelected(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                            )

                            // Trade Requests moved to top section
                        }
                    }

                    // ---------- RIGHT COLUMN ----------
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(
                                rowSpacing,
                                alignment = Alignment.Bottom   // 👈 grow upward
                            )
                        ) {

                            if (phase == TurnPhase.TRADE) {

                                IconActionButton(
                                    icon = Icons.Default.AccountBalance,
                                    contentDescription = "Bank Trade",
                                    onClick = { viewModel.sendBankExchange() },
                                    enabled = canSendBankExchange,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.size(rowHeight)
                                )

                                IconActionButton(
                                    icon = Icons.Default.Group,
                                    contentDescription = "Player Trade",
                                    onClick = { viewModel.sendPlayerExchange() },
                                    enabled = canSendPlayerTrade,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.size(rowHeight)
                                )

                            } else {

                                IconActionButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "End Turn",
                                    onClick = { viewModel.onEndTurn() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.size(rowHeight)
                                )
                            }

                            IconActionButton(
                                icon = Icons.Filled.SwapHoriz,
                                contentDescription = "Trade",
                                onClick = { viewModel.switchTradePanel() },
                                enabled = phase == TurnPhase.TRADE || phase == TurnPhase.MAIN_PHASE,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.size(rowHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}
