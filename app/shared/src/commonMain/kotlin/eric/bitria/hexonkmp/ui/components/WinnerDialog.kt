package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.ui.theme.Spacing

// Full-screen game-over overlay: the winner's card, a headline, and a button back
// to the menu. Shown to everyone once the game reaches the Finished phase.
@Composable
fun WinnerDialog(
    winner: PlayerId,
    players: List<PlayerId>,
    me: PlayerId,
    onReturnToMenu: () -> Unit,
) {
    val youWon = winner == me
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Card {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    if (youWon) "You won!" else "Game over",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                PlayerCard(winner, players, me, current = true, size = 64)
                Text(
                    "${PlayerVisuals.label(winner, players, me)} reached the goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onReturnToMenu) { Text("Return to menu") }
            }
        }
    }
}
