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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.ui.components.game.ControlButton
import eric.bitria.hexon.ui.components.game.OptionsButton
import eric.bitria.hexon.ui.components.game.PlayerTurnBar
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.ui.components.game.assets.BuildingRow
import eric.bitria.hexon.ui.components.game.assets.PlayerResourceRow
import eric.bitria.hexon.ui.components.game.assets.ResourceRow
import eric.bitria.hexon.ui.components.game.assets.TradeResourceRow
import eric.bitria.hexon.viewmodel.game.GameViewModel

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
                    .weight(if (isLandscape) 0.25f else 0.10f)
            ) {
                val sectionSpacing = maxHeight * 0.08f

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {

                    // ---- Top Bar ----
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.6f)
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
                        modifier = Modifier.weight(0.4f)
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
                }
            }

            Spacer(modifier = Modifier.weight(if (isLandscape) 0.2f else 0.65f))

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
                                alignment = Alignment.Bottom   // 👈 grow upward
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

                                ControlButton(
                                    icon = Icons.Default.AccountBalance,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    description = "Bank Trade",
                                    onClick = { viewModel.sendBankExchange() },
                                    enabled = viewModel.canSendBankExchangeBool,
                                    modifier = Modifier.size(rowHeight)
                                )

                                ControlButton(
                                    icon = Icons.Default.Group,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    description = "Trade Players",
                                    onClick = { viewModel.sendPlayerExchange() },
                                    enabled = viewModel.canSendPlayerTrade(),
                                    modifier = Modifier.size(rowHeight)
                                )

                            } else {

                                ControlButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    color = MaterialTheme.colorScheme.primary,
                                    description = "End Turn",
                                    onClick = { viewModel.onEndTurn() },
                                    modifier = Modifier.size(rowHeight)
                                )
                            }

                            ControlButton(
                                icon = Icons.Filled.SwapHoriz,
                                color = MaterialTheme.colorScheme.tertiary,
                                description = "Trade",
                                onClick = { viewModel.switchTradePanel() },
                                enabled = phase == TurnPhase.TRADE || phase == TurnPhase.MAIN_PHASE,
                                modifier = Modifier.size(rowHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}
