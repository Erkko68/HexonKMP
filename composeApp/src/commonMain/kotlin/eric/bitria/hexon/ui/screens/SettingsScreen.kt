package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    paddingScale: Dp
) {
    val height = paddingScale * 0.11f
    val shape = RoundedCornerShape(paddingScale * 0.025f)
    
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onExitClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onDeleteAccountClicked: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val shapes = dimensions.shapes

        if (showLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmation = false },
                title = {
                    Text(
                        "Log Out",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to log out? All your local settings and session data will be cleared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmation = false
                            settingsViewModel.logout()
                        }
                    ) {
                        Text(
                            "LOG OUT",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmation = false }) {
                        Text(
                            "CANCEL",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = shapes.medium
            )
        }

        BoxWithConstraints {
            val isPortrait = maxWidth < maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.screenVertical),
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
                        .padding(horizontal = spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    item {
                        SettingsSection(
                            title = "Game",
                            titleModifier = Modifier
                                .padding(bottom = spacing.small),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = shapes.medium
                                )
                                .padding(spacing.medium)
                        ) {
                            SettingsToggle(
                                label = "Mirror UI",
                                description = "Mirrors the Buttons on the Game Screen, useful for left-handed users",
                                checked = uiState.mirrorUI,
                                onCheckedChange = settingsViewModel::onMirrorUIToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Audio Section
                        SettingsSection(
                            title = "Audio",
                            titleModifier = Modifier
                                .padding(bottom = spacing.small),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = shapes.medium
                                )
                                .padding(spacing.medium)
                        ) {
                            VolumeSlider(
                                label = "Master Volume",
                                value = uiState.masterVolume,
                                onValueChange = settingsViewModel::onMasterVolumeChanged
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            VolumeSlider(
                                label = "Music Volume",
                                value = uiState.musicVolume,
                                onValueChange = settingsViewModel::onMusicVolumeChanged
                            )
                            HorizontalDivider(
                                Modifier.padding(vertical = spacing.small),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            SettingsToggle(
                                label = "Mute All",
                                description = "Mutes all game sounds",
                                checked = uiState.muteAll,
                                onCheckedChange = settingsViewModel::onMuteAllToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Social Section
                        SettingsSection(
                            title = "Social",
                            titleModifier = Modifier
                                .padding(bottom = spacing.small),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = shapes.medium
                                )
                                .padding(spacing.medium)
                        ) {
                            SettingsToggle(
                                label = "Disable Friend Requests",
                                description = "Prevents other players from sending you friend requests",
                                checked = uiState.disableFriendRequests,
                                onCheckedChange = settingsViewModel::onDisableFriendRequestsToggled
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            SettingsToggle(
                                label = "Disable Game Invites",
                                description = "Prevents friends from sending you game invites",
                                checked = uiState.disableGameInvites,
                                onCheckedChange = settingsViewModel::onDisableGameInvitesToggled
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Account Section
                        SettingsSection(
                            title = "Account",
                            titleModifier = Modifier
                                .padding(bottom = spacing.small),
                            sectionModifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = shapes.medium
                                )
                                .padding(spacing.medium)
                        ) {

                            SettingsButton(
                                text = "Log Out",
                                onClick = { showLogoutConfirmation = true },
                                modifier = Modifier.fillMaxWidth(),
                                paddingScale = dimensions.paddingScale
                            )

                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsButton(
                                text = "Change Password",
                                onClick = onChangePasswordClicked,
                                modifier = Modifier.fillMaxWidth(),
                                paddingScale = dimensions.paddingScale
                            )

                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsButton(
                                text = "Delete Account",
                                onClick = onDeleteAccountClicked,
                                modifier = Modifier.fillMaxWidth(),
                                isError = true,
                                paddingScale = dimensions.paddingScale
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))
                    }
                }
            }
        }
    }
}
