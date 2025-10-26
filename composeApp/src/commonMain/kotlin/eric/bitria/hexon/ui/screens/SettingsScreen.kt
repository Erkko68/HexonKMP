package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.settings.SettingsButton
import eric.bitria.hexon.ui.components.settings.SettingsSection
import eric.bitria.hexon.ui.components.settings.SettingsToggle
import eric.bitria.hexon.ui.components.settings.VolumeSlider
import eric.bitria.hexon.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel() },
    onExitClicked: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    HexonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    IconButton(onClick = onExitClicked) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Settings Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Audio Section
                    SettingsSection(title = "Audio") {
                        VolumeSlider(
                            label = "Master Volume",
                            value = uiState.masterVolume,
                            onValueChange = settingsViewModel::onMasterVolumeChanged
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        VolumeSlider(
                            label = "Music Volume",
                            value = uiState.musicVolume,
                            onValueChange = settingsViewModel::onMusicVolumeChanged
                        )
                        HorizontalDivider(
                            Modifier.padding(vertical = 16.dp),
                            DividerDefaults.Thickness, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        SettingsToggle(
                            label = "Mute All",
                            description = "Instantly mutes all game sounds",
                            checked = uiState.muteAll,
                            onCheckedChange = settingsViewModel::onMuteAllToggled
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Social Section
                    SettingsSection(title = "Social") {
                        SettingsToggle(
                            label = "Disable Friend Requests",
                            description = "Prevents other players from sending you friend requests",
                            checked = uiState.disableFriendRequests,
                            onCheckedChange = settingsViewModel::onDisableFriendRequestsToggled
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsToggle(
                            label = "Disable Game Invites",
                            description = "Prevents friends from sending you game invites",
                            checked = uiState.disableGameInvites,
                            onCheckedChange = settingsViewModel::onDisableGameInvitesToggled
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account Section
                    SettingsSection(title = "Account") {
                        SettingsButton(
                            label = "Log Out",
                            onClick = settingsViewModel::onLogOutClicked
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsButton(
                            label = "Delete Account",
                            onClick = settingsViewModel::onDeleteAccountClicked,
                            isDestructive = true
                        )
                    }
                }
            }
        }
    }
}