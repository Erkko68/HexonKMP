package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // --- CHANGE 1: Add import
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // --- CHANGE 2: Add imports
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.Friend
import eric.bitria.hexon.viewmodel.FriendsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel = viewModel { FriendsViewModel() },
    onExitClicked: () -> Unit
) {
    val friends by friendsViewModel.friendsList.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111618))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hexon",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                IconButton(onClick = onExitClicked) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }


            Text(
                text = "FRIENDS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(0.9f).then(
                    Modifier.padding(horizontal = 8.dp)
                )
            ) {

                AddFriendInput(
                    onAddFriend = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friends, key = { it.id }) { friend ->
                        FriendListItem(friend = friend)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendInput(
    onAddFriend: (String) -> Unit // --- CHANGE 6: Add callback parameter
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
            ),
            singleLine = true, // Good practice
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "Add friend by username",
                        color = Color.Gray.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )

        Button(
            onClick = {
                onAddFriend(text)
                text = ""
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1193d4)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Friend",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FriendListItem(friend: Friend) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Avatar and Username
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar/Initial Placeholder
            FriendAvatar(friend = friend)

            Text(
                text = friend.username,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Right Side: Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Invite to Game Button (Blue)
            ActionButton(
                icon = Icons.Default.Gamepad,
                backgroundColor = Color(0xFF1193d4),
                onClick = { /* Invite action */ }
            )

            // View Profile Button (Gray)
            ActionButton(
                icon = Icons.Default.Person,
                backgroundColor = Color.DarkGray.copy(alpha = 0.8f),
                onClick = { /* Profile action */ }
            )
        }
    }
}

@Composable
fun FriendAvatar(friend: Friend) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        if (friend.avatarUrl != null) {

        } else {
            Text(
                text = friend.username.firstOrNull()?.toString() ?: "",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
        }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
fun FriendsScreenPreview() {
    HexonTheme {
        FriendsScreen(
            friendsViewModel = FriendsViewModel(),
            onExitClicked = {}
        )
    }
}