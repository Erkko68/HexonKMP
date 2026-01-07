package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.MainMenuViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainMenuScreen(
    onFriendsClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    onStartGameClicked: () -> Unit,
    viewModel: MainMenuViewModel = koinViewModel(),
    gameSceneViewModel: GameSceneViewModel = koinViewModel(),
) {
    HexonTheme {
        val spacing = HexonTheme.dimensions.spacing

        BoxWithConstraints (
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            GameLayer(
                modifier = Modifier.fillMaxSize(),
                jsonCollector = gameSceneViewModel.sendJson,
                onJsonReceived = gameSceneViewModel::onJsonReceived
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.screenVertical)
                    .clickable(onClick = onStartGameClicked),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    HexonHeader{

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to Start",
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MainMenuScreenPreview() {
    HexonTheme {
        MainMenuScreen(
            onFriendsClicked = {},
            onProfileClicked = {},
            onStartGameClicked = {}
        )
    }
}
