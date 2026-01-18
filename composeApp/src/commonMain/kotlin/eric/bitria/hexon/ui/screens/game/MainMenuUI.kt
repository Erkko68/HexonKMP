package eric.bitria.hexon.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.ui.theme.HexonTheme

@Composable
fun MainMenuUI(
    onFriendsClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    onMatchmakingClicked: () -> Unit,
    onCreateLobbyClicked: () -> Unit,
    isEngineReady: Boolean,
) {
    val spacing = HexonTheme.dimensions.spacing

    AnimatedVisibility(
        visible = isEngineReady,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreateLobbyClicked,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Lobby")
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.screenVertical)
                    .clickable(onClick = onMatchmakingClicked, indication = null, interactionSource = null),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HexonHeader {
                        HexonIconButton.Transparent(
                            onClick = onFriendsClicked,
                            icon = Icons.Default.Group,
                            contentDescription = "Friends"
                        )

                        HexonIconButton.Transparent(
                            onClick = onProfileClicked,
                            icon = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }

                // "Tap to Start" Label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to Search Game",
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
