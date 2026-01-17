package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eric.bitria.hexon.ui.components.game.ControlButton
import eric.bitria.hexon.ui.components.game.ItemCard
import eric.bitria.hexon.ui.components.game.OptionsButton
import eric.bitria.hexon.ui.components.game.PlayerTurn
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.ui.components.game.trade.TradePanel
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.game.GameViewModel
import eric.bitria.hexon.viewmodel.enums.GameUIState
import eric.bitria.hexon.viewmodel.enums.next

@Composable
fun GameUI(
    onExitClicked: () -> Unit,
    viewModel: GameViewModel,
) {
    val players by viewModel.players.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val progressCards by viewModel.progressCards.collectAsState()
    val victoryPoints by viewModel.victoryPoints.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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
                    PlayerTurn(
                        players = players,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .padding(horizontal = spacing.small)
                    )

                    OptionsButton(
                        onExitClicked = onExitClicked,
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
                        victoryPoints = victoryPoints,
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
                if (uiState == GameUIState.TRADING) {
                    TradePanel(
                        players = players,
                        onPlayerClicked = {},
                        modifier = Modifier.height(rowHeight * 1.1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * 2.1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                    ) {
                        Row(
                            modifier = Modifier.height(rowHeight),
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                        ) {
                            val activeItems = if (uiState == GameUIState.TRADING) resources else assets
                            activeItems.forEach { item ->
                                ItemCard(itemCardData = item)
                            }
                        }

                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .height(rowHeight)
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            resources.forEach { resource ->
                                ItemCard(itemCardData = resource)
                            }

                            if (progressCards.isNotEmpty()) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight(0.6f)
                                        .padding(horizontal = spacing.extraSmall),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                progressCards.forEach { progressCard ->
                                    ItemCard(itemCardData = progressCard)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small, Alignment.Bottom)
                    ) {
                        ControlButton(
                            icon = Icons.Filled.SwapHoriz,
                            color = MaterialTheme.colorScheme.tertiary,
                            description = "Trade",
                            onClick = { viewModel.onTradeActionClick() },
                            modifier = Modifier.size(rowHeight)
                        )
                        ControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            color = MaterialTheme.colorScheme.primary,
                            description = "Next Phase",
                            onClick = { viewModel.setUIState(uiState.next())},
                            modifier = Modifier.size(rowHeight)
                        )
                    }
                }
            }
        }
    }
}