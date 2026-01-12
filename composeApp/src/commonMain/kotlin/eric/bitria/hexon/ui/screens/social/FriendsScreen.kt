package eric.bitria.hexon.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.components.friends.AddFriendInput
import eric.bitria.hexon.ui.components.friends.FriendListItem
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.social.FriendsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel = koinViewModel(),
    onExitClicked: () -> Unit,
    onViewProfileClicked: (String) -> Unit
) {
    val uiState by friendsViewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val shapes = dimensions.shapes

        BoxWithConstraints {
            val isPortrait = maxWidth < maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = spacing.screenVertical),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenHorizontal)
                ) {
                    HexonHeader(
                        title = "FRIENDS",
                        isPortrait = isPortrait
                    ) {
                        HexonIconButton.Transparent(
                            onClick = onExitClicked,
                            icon = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { friendsViewModel.refresh() },
                    state = pullToRefreshState,
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                        .padding(horizontal = spacing.screenHorizontal)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        item {
                            AddFriendInput(
                                onAddFriend = { username ->
                                    friendsViewModel.onAddFriendClicked(username)
                                },
                                message = uiState.addFriendMessage,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(spacing.mediumLarge))
                        }

                        // Section: Friends List
                        if (uiState.friendsError != null && uiState.friends.isEmpty()) {
                            item {
                                ErrorState(
                                    message = uiState.friendsError!!,
                                    onRetry = { friendsViewModel.refreshFriends() }
                                )
                            }
                        } else if (uiState.friends.isEmpty() && !uiState.isLoading) {
                            item {
                                EmptyState(text = "No friends added yet")
                            }
                        } else {
                            items(uiState.friends, key = { it.id }) { friend ->
                                FriendListItem(
                                    friend = friend,
                                    onInvite = { /* Handle invite */ },
                                    onViewProfile = { onViewProfileClicked(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensions.listItemHeight)
                                        .clip(shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                )
                            }
                        }

                        // Section: Friend Requests
                        if (uiState.friendRequests.isNotEmpty() || uiState.requestsError != null) {
                            item {
                                Spacer(modifier = Modifier.height(spacing.medium))
                                Text(
                                    text = "Friend Requests",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.small)
                                )
                                Spacer(modifier = Modifier.height(spacing.small))
                            }

                            if (uiState.requestsError != null) {
                                item {
                                    ErrorState(
                                        message = uiState.requestsError!!,
                                        onRetry = { friendsViewModel.refreshRequests() }
                                    )
                                }
                            } else {
                                items(uiState.friendRequests, key = { "request_${it.id}" }) { request ->
                                    FriendListItem(
                                        friend = request,
                                        onAccept = { friendsViewModel.onAcceptRequest(it) },
                                        onDecline = { friendsViewModel.onDeclineRequest(it) },
                                        onViewProfile = { onViewProfileClicked(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(dimensions.listItemHeight)
                                            .clip(shapes.medium)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = HexonTheme.dimensions.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = HexonTheme.dimensions.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        HexonIconButton.Transparent(
            onClick = onRetry,
            icon = Icons.Default.Refresh,
            contentDescription = "Retry"
        )
    }
}
