package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// A held achievement (Longest Road / Largest Army) shown as a fixed-size yellow
// token with just an icon — no count, and not tappable (achievements are passive,
// reassigned automatically by the engine). Same footprint as a DevelopmentCard so
// it lines up in the development bar.
@Composable
fun AchievementCard(
    painter: Painter,
    label: String,
) {
    val bg = Color(0xFFFBC02D) // amber/yellow — the achievement accent
    val on = Color.Black
    Card(
        modifier = Modifier.size(Tokens.tokenMd),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = on),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(0.55f),
            )
        }
    }
}
