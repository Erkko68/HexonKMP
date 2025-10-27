package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.profile.GameHistoryList
import eric.bitria.hexon.ui.components.profile.UserInfoSection
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.FriendProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendProfileScreen(
    username: String = "Guest",
    profileViewModel: FriendProfileViewModel = koinViewModel(),
    onExitClicked: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(username) {
        profileViewModel.loadFriendProfile(username)
    }

    HexonTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            HexonHeader {

                HexonIconButton.Transparent(
                    onClick = onExitClicked,
                    icon = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            UserInfoSection(
                username = uiState.username,
                avatarUrl = uiState.avatarUrl,
                stats = uiState.stats
            )

            Spacer(modifier = Modifier.height(40.dp))

            GameHistoryList(history = uiState.gameHistory)
        }
    }
}
