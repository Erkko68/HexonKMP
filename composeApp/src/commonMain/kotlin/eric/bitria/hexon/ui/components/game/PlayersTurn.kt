package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Player(
    val name: String,
    val color: Color
)

@Composable
fun PlayerIcon(player: Player, isActive: Boolean = false) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.Black else Color.Transparent)
            .clickable { showDialog = !showDialog },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "${player.name} Avatar",
            tint = player.color.copy(alpha = if (isActive) 1f else 0.6f),
            modifier = Modifier.size(36.dp)
        )

        // Show small popup below the icon when clicked
        if (showDialog) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 50) // position below the icon
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable { showDialog = false } // dismiss on click
                ) {
                    Text(
                        text = player.name,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerTurn(players: List<Player>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First player: black background (active)
        PlayerIcon(player = players.first(), isActive = true)

        // Other players
        players.drop(1).forEach { player ->
            PlayerIcon(player = player)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlayerTurnRowPreview() {
    val players = listOf(
        Player("Player 1", Color(0xFF81D4FA)), // Active
        Player("Player 2", Color(0xFFE57373)),
        Player("Player 3", Color(0xFF81C784)),
        Player("Player 4", Color(0xFFFFB74D))
    )

    PlayerTurn(players = players)
}
