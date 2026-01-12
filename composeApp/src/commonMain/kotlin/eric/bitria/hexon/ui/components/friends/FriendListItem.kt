package eric.bitria.hexon.ui.components.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val dimensions = HexonTheme.dimensions
    val spacing = dimensions.spacing
    val shapes = dimensions.shapes
    val vividColor = friend.username.toVividColor()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.listItemHeight),
        shape = shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            0.0f to vividColor.copy(alpha = 0.35f),
                            0.5f to Color.Transparent
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main info area (clickable)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onViewProfile(friend.id) }
                        .padding(start = spacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.username,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .padding(end = spacing.medium)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    if (onInvite != null) {
                        FilledIconButton(
                            onClick = { onInvite(friend.username) },
                            modifier = Modifier.size(dimensions.listItemHeight * 0.6f),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = "Invite Friend",
                                modifier = Modifier.fillMaxSize(0.6f)
                            )
                        }
                    }

                    if (onAccept != null) {
                        FilledIconButton(
                            onClick = { onAccept(friend.username) },
                            modifier = Modifier.size(dimensions.listItemHeight * 0.6f),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.fillMaxSize(0.6f)
                            )
                        }
                    }

                    if (onDecline != null) {
                        FilledIconButton(
                            onClick = { onDecline(friend.username) },
                            modifier = Modifier.size(dimensions.listItemHeight * 0.6f),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Decline",
                                modifier = Modifier.fillMaxSize(0.6f)
                            )
                        }
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
            onViewProfile = {  }
        )
    }
}
