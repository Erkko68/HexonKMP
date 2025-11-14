package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.game.ControlButton
import eric.bitria.hexon.ui.components.game.ItemCards
import eric.bitria.hexon.ui.components.game.OptionsButton
import eric.bitria.hexon.ui.components.game.PlayerTurn
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.enums.GameUIState
import eric.bitria.hexon.viewmodel.enums.next
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(
    onExitClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    gameSceneViewModel: GameSceneViewModel = koinViewModel(),
    gameUIViewModel: GameUIViewModel = koinViewModel(),
) {
    // Variable States
    val players by gameUIViewModel.players.collectAsState()
    val resources by gameUIViewModel.resources.collectAsState()
    val assets by gameUIViewModel.assets.collectAsState()
    val victoryPoints by gameUIViewModel.victoryPoints.collectAsState()
    val uiState by gameUIViewModel.uiState.collectAsState()

    HexonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            GameLayer(
                modifier = Modifier.fillMaxSize(),
                jsonCollector = gameSceneViewModel.sendJson,
                onJsonReceived = gameSceneViewModel::onJsonReceived
            )

            // UI Layer
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp))
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerTurn(
                            players = players,
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        OptionsButton(
                            onExitClicked = { onExitClicked() },
                            onAboutClicked = { onAboutClicked() }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VictoryPointsIndicator(victoryPoints)
                    }
                }

                // Below section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                )
                {
                    // Left Column (Build actions & resources)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
                    ) {
                        if (uiState == GameUIState.TRADING){
                            ItemCards( // Player Resources
                                items = resources
                            )
                        } else {
                            ItemCards( // Player Remaining Buildings and Development Cards
                                items = assets
                            )
                        }
                        ItemCards( // Player Resources
                            items = resources
                        )
                    }

                    // Right Column (Turn controls)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.Bottom
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { gameUIViewModel.setUIState(uiState.next()) },
                            description = "End Turn",
                            color = Color(0xFF2196F3).copy(alpha = 0.8f),
                            iconSize = 30.dp
                        )
                        ControlButton(
                            icon = Icons.Filled.SwapHoriz,
                            onClick = { gameUIViewModel.onTradeActionClick() },
                            description = "Trade",
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                            iconSize = 30.dp
                        )
                        ControlButton(
                            icon = Icons.Filled.SwapHoriz,
                            onClick = {},
                            description = "Trade",
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                            iconSize = 30.dp
                        )
                    }
                }
            }
        }
    }
}
