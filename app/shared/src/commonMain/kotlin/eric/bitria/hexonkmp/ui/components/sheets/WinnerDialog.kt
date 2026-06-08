package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// Full-screen game-over overlay. [color] and [label] are the winner's resolved
// player visuals; [youWon] drives the headline copy.
@Composable
fun WinnerDialog(
    color: Color,
    label: String,
    youWon: Boolean,
    onReturnToMenu: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Card {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    stringResource(if (youWon) Res.string.you_won else Res.string.game_over),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                PlayerToken(color, label, size = Tokens.tokenLg)
                Text(
                    stringResource(Res.string.reached_goal, label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onReturnToMenu) { Text(stringResource(Res.string.return_to_menu)) }
            }
        }
    }
}
