package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import eric.bitria.hexon.ui.components.game.trade.TradePanel
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.enums.GameUIState
import eric.bitria.hexon.viewmodel.enums.next
import org.jetbrains.compose.ui.tooling.preview.Preview
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
        BoxWithConstraints (
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            val paddingScale = minOf(maxWidth, maxHeight)

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.08f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                )
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                            .fillMaxWidth()
                            .weight(0.6f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VictoryPointsIndicator(victoryPoints)
                    }
                }

                // Below section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                )
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.33f),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiState == GameUIState.TRADING) {
                            TradePanel(
                                players = players,
                                onPlayerClicked = {}
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.66f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left Column (Build actions & resources)
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(paddingScale * 0.02f),
                            verticalArrangement = Arrangement.spacedBy(paddingScale * 0.02f, Alignment.Bottom)
                        ) {
                            if (uiState == GameUIState.TRADING) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Start
                                ){
                                    ItemCards(
                                        items = resources
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Start,
                                ){
                                    ItemCards(
                                        items = assets
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Start,
                            ){
                                ItemCards( // Player Resources
                                    items = resources
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(paddingScale * 0.02f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(paddingScale * 0.02f, Alignment.Bottom)
                        ){
                            ControlButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                color = Color(0xFF2196F3),
                                description = "Play",
                                onClick = { gameUIViewModel.setUIState(uiState.next()) },
                                modifier = Modifier
                                    .weight(1f)
                            )
                            ControlButton(
                                icon = Icons.Filled.SwapHoriz,
                                color = Color(0xFF4CAF50),
                                description = "Trade",
                                onClick = { gameUIViewModel.onTradeActionClick() },
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GameScreenPreview(){
    // to preview comment GameLayer
    GameScreen(
        onExitClicked = {},
        onAboutClicked = {},
        gameSceneViewModel = GameSceneViewModel(),
        gameUIViewModel = GameUIViewModel(),
    )
}