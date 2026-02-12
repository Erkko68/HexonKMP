package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.ui.components.game.ControlButton
import eric.bitria.hexon.ui.components.game.OptionsButton
import eric.bitria.hexon.ui.components.game.PlayerTurnBar
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.ui.components.game.assets.BuildingRow
import eric.bitria.hexon.ui.components.game.assets.PlayerResourceRow
import eric.bitria.hexon.ui.components.game.assets.ResourceRow
import eric.bitria.hexon.ui.components.game.assets.TradeResourceRow
import eric.bitria.hexon.ui.theme.HexonTheme
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
    //val progressCards by viewModel.progressCards.collectAsState()
    val me by viewModel.me.collectAsState()

    val offeredResources by viewModel.offeredResources.collectAsState()
    val requestedResources by viewModel.requestedResources.collectAsState()

    val phase by viewModel.turnPhase.collectAsState()
    val activePlayerId by viewModel.activePlayerId.collectAsState()

    val dimensions = HexonTheme.dimensions
    val spacing = dimensions.spacing
    val rowHeight = dimensions.listItemHeight * 0.7f

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = spacing.screenHorizontal,
                    vertical = spacing.screenVertical * 1.5f
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(CircleShape),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerTurnBar(
                        me = me,
                        opponents = players,
                        activePlayerId = activePlayerId,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .padding(horizontal = spacing.small)
                    )

                    OptionsButton(
                        onExitClicked = {
                            onExitClicked()
                            viewModel.onExitGame()
                        },
                        onAboutClicked = {},
                        modifier = Modifier.fillMaxHeight()
                            .padding(end = spacing.small)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    VictoryPointsIndicator(
                        victoryPoints = Pair(me?.victoryPoints ?: 0,maxVictoryPoints),
                        modifier = Modifier.height(rowHeight * 0.65f)
                    )
                }
            }

            // Bottom Group
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * 4.1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                    ) {
                        if (phase == TurnPhase.TRADE){
                            ResourceRow(
                                resources = resources,
                                onClick = { resourceId -> viewModel.onRequestedResourceSelected(resourceId) },
                                modifier = Modifier.height(rowHeight)
                            )
                            TradeResourceRow(
                                selected = requestedResources,
                                onClick = { resourceId -> viewModel.onRequestedResourceDeselected(resourceId) },
                                modifier = Modifier.height(rowHeight)
                            )
                            TradeResourceRow(
                                selected = offeredResources,
                                onClick = { resourceId -> viewModel.onOfferedResourceDeselected(resourceId) },
                                modifier = Modifier.height(rowHeight)
                            )
                        } else {
                            BuildingRow(
                                buildings = buildings,
                                onClick = { buildingId -> viewModel.showAvailableBuildingPositions(buildingId) },
                                modifier = Modifier.height(rowHeight)
                            )
                        }

                        // --- Resources Row  ---
                        PlayerResourceRow(
                            me = me,
                            selected = offeredResources,
                            onClick = { resourceId -> viewModel.onOfferedResourceSelected(resourceId) },
                            modifier = Modifier.height(rowHeight)
                        )

                    }

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small, Alignment.Bottom)
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
                                description = "Trade with Players",
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
                            modifier = Modifier.size(rowHeight)
                        )
                    }
                }
            }
        }
    }
}