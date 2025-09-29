package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.GameViewModel
import eric.bitria.hexon.viewmodel.MainMenuViewModel

@Composable
fun MainMenuScreen(
    onFriendsClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    viewModel: MainMenuViewModel = viewModel { MainMenuViewModel() },
    gameViewModel: GameViewModel = viewModel { GameViewModel() },
) {
    HexonTheme {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            GameLayer(
                modifier = Modifier.fillMaxSize(),
                jsonCollector = gameViewModel.sendJson,
                onJsonReceived = gameViewModel::onJsonReceived
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hexon",
                        style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onFriendsClicked() }) {
                            Icon(
                                imageVector = Icons.Default.Group, // replace with proper Group icon if needed
                                contentDescription = "Friends",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onProfileClicked() }) {
                            Icon(
                                imageVector = Icons.Default.Person, // replace with proper Person icon if needed
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Main map selector + actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 96.dp)
                ) {
                    // Previous map button
                    IconButton(
                        onClick = { /* previous map */ },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Map Name",
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                    )

                    // Next map button
                    IconButton(
                        onClick = { /* next map */ },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward, // replace with forward arrow icon
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 64.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .clickable { /* action */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }


                // Footer "Tap to Start"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .clickable { viewModel.onStartGame() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to Start",
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}