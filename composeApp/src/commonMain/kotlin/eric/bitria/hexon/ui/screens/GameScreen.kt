package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.ui.components.game.ControlButton
import eric.bitria.hexon.ui.components.game.ItemCardData
import eric.bitria.hexon.ui.components.game.ItemCards
import eric.bitria.hexon.ui.components.game.Player
import eric.bitria.hexon.ui.components.game.PlayerTurnFlow
import eric.bitria.hexon.ui.components.game.VictoryPointsIndicator
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.GameViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = koinViewModel(),
    gameSceneViewModel: GameSceneViewModel = koinViewModel(),
    gameUIViewModel: GameUIViewModel = koinViewModel(),
) {
    val gameEvents = gameSceneViewModel.gameEvents.collectAsState(initial = "Waiting for events...")

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
                    PlayerTurnFlow(
                        players = listOf(
                            Player("Player 1", Color(0xFF81D4FA)),
                            Player("Player 2", Color(0xFFE57373)),
                            Player("Player 3", Color(0xFF81C784)),
                            Player("Player 4", Color(0xFFFFB74D))
                        ),
                        modifier = Modifier.wrapContentWidth()
                    )

                    // Options button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "More options",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VictoryPointsIndicator()
                }
            }

            // Below section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                    verticalAlignment = Alignment.Bottom // items-end
                ) {
                    // Left Column (Build actions & resources)
                    Column(
                        modifier = Modifier.weight(1f), // 1fr
                        verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
                    ) {
                        ItemCards(
                            listOf(
                                ItemCardData(
                                    "1",
                                    Icons.Filled.Castle,
                                    "Castle",
                                    Color(0xFF81D4FA),
                                    Color(0xFFB3E5FC)
                                ),
                            )
                        )
                        ItemCards(
                            listOf(
                                ItemCardData(
                                    "4",
                                    Icons.Filled.LocalFlorist,
                                    "Wool",
                                    Color(0xFFBA68C8),
                                    Color(0xFFCE93D8)
                                )
                            )
                        )
                    }
                    // Right Column (Turn controls)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.Bottom
                        ), // gap-2, justify-end
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { gameSceneViewModel.testCommand() },
                            description = "End Turn",
                            color = Color(0xFF2196F3).copy(alpha = 0.8f), // bg-blue-500/80
                            iconSize = 30.dp // text-4xl is large, 30.dp fits better
                        )
                        ControlButton(
                            icon = Icons.Filled.SwapHoriz,
                            onClick = {},
                            description = "Trade",
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f), // bg-green-500/80
                            iconSize = 30.dp // text-3xl
                        )
                        ControlButton(
                            icon = Icons.Filled.SwapHoriz,
                            onClick = {},
                            description = "Trade",
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f), // bg-green-500/80
                            iconSize = 30.dp // text-3xl
                        )
                    }
                }
            }
        }
    }
}
