package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.friends.AddFriendInput
import eric.bitria.hexon.ui.components.friends.FriendListItem
import eric.bitria.hexon.ui.components.shared.HexonHeader
import eric.bitria.hexon.ui.components.shared.HexonIconButton
import eric.bitria.hexon.viewmodel.FriendsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel = viewModel { FriendsViewModel() },
    onExitClicked: () -> Unit,
    onViewProfileClicked: (String) -> Unit
) {
    val friends by friendsViewModel.friendsList.collectAsState()

    HexonTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            HexonHeader(
                title = "FRIENDS"
            ) {
                HexonIconButton.Transparent(
                    onClick = onExitClicked,
                    icon = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {

                AddFriendInput(
                    onAddFriend = { username ->
                        /* TODO: Handle add friend action */
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friends, key = { it.id }) { friend ->
                        FriendListItem(
                            friend = friend,
                            onInvite = { /*TODO*/ },
                            onViewProfile = {
                                onViewProfileClicked(friend.username)
                            }
                        )
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