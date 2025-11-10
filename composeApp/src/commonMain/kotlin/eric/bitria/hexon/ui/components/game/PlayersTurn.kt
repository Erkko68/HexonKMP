package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Player(
    val name: String,
    val tradesEnabled: Boolean,
    val color: Color
)

@Composable
fun PlayerIcon(
    player: Player,
    isActive: Boolean = false,
    onToggleTrades: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "${player.name} Avatar",
                tint = player.color.copy(alpha = if (isActive) 1f else 0.6f),
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = if (isActive) 3.dp else 0.dp,
                        shape = CircleShape,
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
            )
        }

        PlayerDropdownMenu(
            player = player,
            expanded = expanded,
            onDismiss = { expanded = false },
            onToggleTrades = onToggleTrades
        )
    }
}

@Composable
private fun PlayerDropdownMenu(
    player: Player,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onToggleTrades: (Boolean) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        // Player name (informative)
        DropdownMenuItem(
            text = {
                Text(
                    text = player.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onDismiss,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // Trade toggle
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Trade",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = player.tradesEnabled,
                        onCheckedChange = { onToggleTrades(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.Gray
                        )
                    )
                }
            },
            onClick = {}, // handled by Switch
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PlayerTurn(players: List<Player>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First player: black background (active)
        PlayerIcon(
            player = players.first(),
            isActive = true,
            onToggleTrades = { /* Handle trade toggle */ },
        )

        // Other players
        players.drop(1).forEach { player ->
            PlayerIcon(
                player = player,
                onToggleTrades = { /* Handle trade toggle */ }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerTurnRowPreview() {
    val players = listOf(
        Player("Player 1", true, Color(0xFF81D4FA)), // Active
        Player("Player 2", true, Color(0xFFE57373)),
        Player("Player 3", true, Color(0xFF81C784)),
        Player("Player 4", true, Color(0xFFFFB74D))
    )

    PlayerTurn(players = players)
}
