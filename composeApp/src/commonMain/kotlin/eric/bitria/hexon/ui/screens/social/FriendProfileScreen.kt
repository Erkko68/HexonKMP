package eric.bitria.hexon.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.ui.components.profile.UserInfoSection
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.utils.toVividColor
import eric.bitria.hexon.viewmodel.social.FriendProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendProfileScreen(
    userId: String = "Guest",
    profileViewModel: FriendProfileViewModel = koinViewModel(),
    onExitClicked: () -> Unit
) {
    val state = profileViewModel.state

    LaunchedEffect(userId) {
        profileViewModel.loadFriendProfile(userId)
    }

    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing

        BoxWithConstraints {
            val isPortrait = maxWidth < maxHeight

            // Default color if loading or error
            val vividColor = if (state is ApiResult.Success) {
                state.data.username.toVividColor()
            } else {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to vividColor.copy(alpha = 0.35f),
                            0.6f to Color.Transparent
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.screenVertical),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HexonHeader {
                        HexonIconButton.Transparent(
                            onClick = onExitClicked,
                            icon = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }

                    when (state) {
                        is ApiResult.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ApiResult.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(spacing.large),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = state.message ?: "User not found",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(spacing.medium))
                                HexonIconButton.Transparent(
                                    onClick = { profileViewModel.loadFriendProfile(userId) },
                                    icon = Icons.Default.Refresh,
                                    contentDescription = "Retry"
                                )
                            }
                        }
                        is ApiResult.NetworkError -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(spacing.large),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Network error. Please check your connection.",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(spacing.medium))
                                HexonIconButton.Transparent(
                                    onClick = { profileViewModel.loadFriendProfile(userId) },
                                    icon = Icons.Default.Refresh,
                                    contentDescription = "Retry"
                                )
                            }
                        }
                        is ApiResult.Success -> {
                            val profile = state.data
                            val uiStats = profileViewModel.getProcessedStats(profile)

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                                    .padding(horizontal = spacing.small),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                item {
                                    if (isPortrait) {
                                        Spacer(modifier = Modifier.height(spacing.medium))
                                    }

                                    UserInfoSection(
                                        username = profile.username,
                                        stats = uiStats
                                    )

                                    Spacer(modifier = Modifier.height(spacing.large))

                                    Text(
                                        text = "Game History",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(spacing.medium))
                                }
//                                items(profileViewModel.gameHistory, key = { it.id }) { item ->
//                                    GameHistoryCard(
//                                        item = item,
//                                        modifier = Modifier
//                                            .height(dimensions.listItemHeight)
//                                            .fillMaxWidth()
//                                    )
//                                    Spacer(modifier = Modifier.height(spacing.medium))
//                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
