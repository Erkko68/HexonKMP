package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.settings.SettingsSection
import eric.bitria.hexon.ui.components.settings.SettingsToggle
import eric.bitria.hexon.ui.components.settings.VolumeSlider
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onExitClicked: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    HexonTheme {
        BoxWithConstraints {
            val isPortrait = maxWidth < maxHeight
            val paddingScale = minOf(maxWidth, maxHeight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingScale * 0.04f, vertical = paddingScale * 0.02f)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                HexonHeader(
                    title = "Settings",
                    isPortrait = isPortrait
                ) {
                    HexonIconButton.Transparent(
                        onClick = onExitClicked,
                        icon = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }

                // Settings Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                        .padding(horizontal = paddingScale * 0.02f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    item {
                        SettingsSection(
                            title = "Game",
                            titleModifier = Modifier
                                .padding(bottom = paddingScale * 0.02f),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(paddingScale * 0.02f)
                                )
                                .padding(paddingScale * 0.04f)
                        ) {
                            SettingsToggle(
                                label = "Mirror UI",
                                description = "Mirrors the Buttons on the Game Screen, useful for left-handed users",
                                checked = uiState.mirrorUI,
                                onCheckedChange = settingsViewModel::onMirrorUIToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(paddingScale * 0.04f))

                        // Audio Section
                        SettingsSection(
                            title = "Audio",
                            titleModifier = Modifier
                                .padding(bottom = paddingScale * 0.02f),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(paddingScale * 0.02f)
                                )
                                .padding(paddingScale * 0.04f)
                        ) {
                            VolumeSlider(
                                label = "Master Volume",
                                value = uiState.masterVolume,
                                onValueChange = settingsViewModel::onMasterVolumeChanged
                            )
                            Spacer(modifier = Modifier.height(paddingScale * 0.02f))
                            VolumeSlider(
                                label = "Music Volume",
                                value = uiState.musicVolume,
                                onValueChange = settingsViewModel::onMusicVolumeChanged
                            )
                            HorizontalDivider(
                                Modifier.padding(vertical = paddingScale * 0.02f),
                                DividerDefaults.Thickness,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            SettingsToggle(
                                label = "Mute All",
                                description = "Mutes all game sounds",
                                checked = uiState.muteAll,
                                onCheckedChange = settingsViewModel::onMuteAllToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(paddingScale * 0.04f))

                        // Social Section
                        SettingsSection(
                            title = "Social",
                            titleModifier = Modifier
                                .padding(bottom = paddingScale * 0.02f),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(paddingScale * 0.02f)
                                )
                                .padding(paddingScale * 0.04f)
                        ) {
                            SettingsToggle(
                                label = "Disable Friend Requests",
                                description = "Prevents other players from sending you friend requests",
                                checked = uiState.disableFriendRequests,
                                onCheckedChange = settingsViewModel::onDisableFriendRequestsToggled
                            )
                            Spacer(modifier = Modifier.height(paddingScale * 0.02f))
                            SettingsToggle(
                                label = "Disable Game Invites",
                                description = "Prevents friends from sending you game invites",
                                checked = uiState.disableGameInvites,
                                onCheckedChange = settingsViewModel::onDisableGameInvitesToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(paddingScale * 0.04f))

                        // Account Section
                        SettingsSection(
                            title = "Account",
                            titleModifier = Modifier
                                .padding(bottom = paddingScale * 0.02f),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(paddingScale * 0.02f)
                                )
                                .padding(paddingScale * 0.04f)
                        ) {
                            TextButton(
                                onClick = settingsViewModel::onLogOutClicked,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors()
                            ) {
                                Text(
                                    text = "Log Out",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(paddingScale * 0.01f))

                            TextButton(
                                onClick = settingsViewModel::onDeleteAccountClicked,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors()
                            ) {
                                Text(
                                    text = "Delete Account",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(paddingScale * 0.04f))
                    }
                }
            }
        }
    }
}