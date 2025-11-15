package eric.bitria.hexon.ui.components.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.data.Friend
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FriendListItem(
    friend: Friend,
    onInvite: (String) -> Unit,
    onViewProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {

        val paddingScale = minOf(maxWidth, maxHeight)

        Row(
            modifier = modifier
        ){
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onViewProfile(friend.username) }
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(paddingScale * 0.05f)
            ) {

                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = "${friend.username} Avatar",
                    modifier = Modifier
                        .clip(CircleShape)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )

                Text(
                    text = friend.username,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    autoSize = null,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { onInvite(friend.username) },
                modifier = Modifier
                    .clip(CircleShape)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = "Invite Friend to Play",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview
@Composable
fun FriendListItemPreview(){
    HexonTheme {
        FriendListItem(
            friend = Friend(1,"Pato",true,),
            onInvite = {  },
            onViewProfile = {  },
            modifier = Modifier
                .width(300.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(4.dp)
        )
    }
}