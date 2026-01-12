package eric.bitria.hexon.ui.components.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.utils.toVividColor
import eric.bitria.hexon.viewmodel.data.Friend
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FriendListItem(
    friend: Friend,
    onInvite: ((String) -> Unit)? = null,
    onAccept: ((String) -> Unit)? = null,
    onDecline: ((String) -> Unit)? = null,
    onViewProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val paddingScale = dimensions.paddingScale
        val vividColor = friend.username.toVividColor()

        Row(
            modifier = modifier
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to vividColor.copy(alpha = 0.35f),
                        0.5f to Color.Transparent, 
                        startX = 0f,
                        endX = constraints.maxWidth.toFloat()
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onViewProfile(friend.username) }
                    .fillMaxHeight()
                    .padding(start = spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = friend.username,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (onInvite != null) {
                IconButton(
                    onClick = { onInvite(friend.username) },
                    modifier = Modifier
                        .padding(end = spacing.medium)
                        .size(paddingScale * 0.06f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Invite Friend to Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(paddingScale * 0.055f)
                    )
                }
            }

            if (onAccept != null && onDecline != null) {
                Row(
                    modifier = Modifier.padding(end = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    IconButton(
                        onClick = { onAccept(friend.username) },
                        modifier = Modifier
                            .size(paddingScale * 0.06f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept Friend Request",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(paddingScale * 0.055f)
                        )
                    }

                    IconButton(
                        onClick = { onDecline(friend.username) },
                        modifier = Modifier
                            .size(paddingScale * 0.06f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decline Friend Request",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(paddingScale * 0.055f)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FriendListItemPreview(){
    HexonTheme {
        FriendListItem(
            friend = Friend("1","Pato",true,),
            onInvite = {  },
            onViewProfile = {  },
            modifier = Modifier
                .width(300.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )
    }
}
