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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.components.profile.GameHistoryCard
import eric.bitria.hexon.ui.components.profile.UserInfoSection
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.ui.utils.toVividColor
import eric.bitria.hexon.viewmodel.social.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = koinViewModel(),
    onSettingsClicked: () -> Unit,
    onExitClicked: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val vividColor = uiState.username.toVividColor()
    val pullToRefreshState = rememberPullToRefreshState()

    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing

        BoxWithConstraints {
            val isPortrait = maxWidth < maxHeight

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to vividColor.copy(alpha = 0.35f),
                            0.5f to Color.Transparent
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
                            onClick = onSettingsClicked,
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                        HexonIconButton.Transparent(
                            onClick = onExitClicked,
                            icon = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }

                    if (isLoading && uiState.username.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.error != null && uiState.username.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(spacing.medium))
                            HexonIconButton.Transparent(
                                onClick = { profileViewModel.retry() },
                                icon = Icons.Default.Refresh,
                                contentDescription = "Retry"
                            )
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isLoading,
                            onRefresh = { profileViewModel.retry() },
                            state = pullToRefreshState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                                    .padding(horizontal = spacing.small)
                                    .align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                item {
                                    if (isPortrait) {
                                        Spacer(modifier = Modifier.height(spacing.medium))
                                    }

                                    UserInfoSection(
                                        username = uiState.username,
                                        stats = uiState.stats
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
                                items(uiState.gameHistory, key = { it.id }) { item ->
                                    GameHistoryCard(
                                        item = item,
                                        modifier = Modifier
                                            .height(dimensions.listItemHeight)
                                            .fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(spacing.medium))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}