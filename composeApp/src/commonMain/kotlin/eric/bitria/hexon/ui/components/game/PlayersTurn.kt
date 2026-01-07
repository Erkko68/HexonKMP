package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.PopupProperties
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.data.Player
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PlayerIcon(
    player: Player,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onToggleTrades: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .clickable { expanded = !expanded },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "${player.name} Avatar",
            tint = player.color.copy(alpha = if (isActive) 1f else 0.5f),
            modifier = Modifier.fillMaxSize()
        )

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
    val shapes = HexonTheme.dimensions.shapes
    val spacing = HexonTheme.dimensions.spacing
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(shapes.medium)
            .background(Color.Black.copy(alpha = 0.6f))
            .border(
                width = spacing.extraSmall * 0.5f,
                color = Color.White.copy(alpha = 0.1f),
                shape = shapes.medium
            ),
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        // Player name (informative)
        DropdownMenuItem(
            text = {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            },
            onClick = onDismiss,
        )

        // Trade toggle
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HexonTheme.dimensions.spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Trade",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
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
        )
    }
}

@Composable
fun PlayerTurn(players: List<Player>, modifier: Modifier = Modifier) {
    val spacing = HexonTheme.dimensions.spacing
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        players.forEachIndexed { index, player ->
            PlayerIcon(
                player = player,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                isActive = index == 0, // Assuming first is active for now
                onToggleTrades = { /* Handle trade toggle */ },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerTurnRowPreview() {
    HexonTheme {
        val players = listOf(
            Player("Player 1", true, Color(0xFF81D4FA)), // Active
            Player("Player 2", true, Color(0xFFE57373)),
            Player("Player 3", true, Color(0xFF81C784)),
            Player("Player 4", true, Color(0xFFFFB74D))
        )

        PlayerTurn(players = players)
    }
}
