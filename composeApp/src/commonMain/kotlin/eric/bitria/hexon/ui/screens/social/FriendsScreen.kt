package eric.bitria.hexon.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.friends.AddFriendInput
import eric.bitria.hexon.ui.components.friends.FriendListItem
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.social.FriendsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel = koinViewModel(),
    onExitClicked: () -> Unit,
    onViewProfileClicked: (String) -> Unit
) {
    val friends by friendsViewModel.friendsList.collectAsState()

    HexonTheme {

        BoxWithConstraints {

            val paddingScale = minOf(maxWidth, maxHeight)
            val listItemSize = maxOf(maxWidth, maxHeight) * 0.08f
            val isPortrait = maxWidth < maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = paddingScale * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = paddingScale * 0.04f)
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                        .padding(horizontal = paddingScale * 0.04f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    AddFriendInput(
                        onAddFriend = { username ->
                            friendsViewModel.onAddFriendClicked(username)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listItemSize * 0.8f)
                            .clip(RoundedCornerShape(paddingScale * 0.04f))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(paddingScale * 0.05f))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = paddingScale * 0.04f),
                        verticalArrangement = Arrangement.spacedBy(paddingScale * 0.02f)
                    ) {
                        items(friends, key = { it.id }) { friend ->
                            FriendListItem(
                                friend = friend,
                                onInvite = { friendsViewModel.onInviteClicked(it) },
                                onViewProfile = { onViewProfileClicked(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(listItemSize)
                                    .clip(RoundedCornerShape(paddingScale * 0.04f))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(paddingScale * 0.04f))
                            Text(
                                "Friend Requests",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(paddingScale * 0.02f))
                        }

                        items(friends, key = { it.username }) { friend ->
                            FriendListItem(
                                friend = friend,
                                onInvite = { friendsViewModel.onInviteClicked(it) },
                                onViewProfile = { onViewProfileClicked(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(listItemSize)
                                    .clip(RoundedCornerShape(paddingScale * 0.04f))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
fun FriendsScreenPreview() {
    HexonTheme {
        FriendsScreen(
            friendsViewModel = FriendsViewModel(),
            onExitClicked = {},
            onViewProfileClicked = {}
        )
    }
}
