package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Player(
    val name: String,
    val color: Color
)

@Composable
fun ActivePlayerBadge(player: Player) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black)
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "${player.name} Avatar",
            tint = player.color,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = player.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}


// Inactive player icon (no name, faded)
@Composable
fun InactivePlayerIcon(player: Player) {
    Icon(
        imageVector = Icons.Filled.AccountCircle,
        contentDescription = "${player.name} Avatar",
        tint = player.color.copy(alpha = 0.5f),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .size(36.dp)
            .alpha(0.5f)
    )
}

@Composable
fun PlayerTurnFlow(players: List<Player>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .clip(CircleShape)
            .shadow(4.dp, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        // Active player first
        ActivePlayerBadge(player = players.first())

        // Other players
        players.drop(1).forEach { player ->
            InactivePlayerIcon(player = player)
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

    PlayerTurnFlow(players = players)
}
