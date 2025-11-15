package eric.bitria.hexon.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.friends.AddFriendInput
import eric.bitria.hexon.ui.components.friends.FriendListItem
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.FriendsViewModel
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
                    .padding(paddingScale * 0.04f),
                horizontalAlignment = Alignment.CenterHorizontally
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 1f else 0.5f)
                        .padding(horizontal = paddingScale * 0.02f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    AddFriendInput(
                        onAddFriend = { username ->
                            friendsViewModel.onAddFriendClicked(username)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(paddingScale * 0.04f))
                            .fillMaxWidth()
                            .height(listItemSize)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                top = paddingScale * 0.02f,
                                bottom = paddingScale * 0.02f,
                                start = paddingScale * 0.04f,
                                end = paddingScale * 0.02f
                            )
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
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(paddingScale * 0.02f)
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