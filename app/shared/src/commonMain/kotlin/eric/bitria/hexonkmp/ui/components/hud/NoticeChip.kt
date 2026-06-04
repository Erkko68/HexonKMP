package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing

private val GreenContainer = Color(0xFF1B5E20).copy(alpha = 0.85f)
private val GreenOnContainer = Color(0xFFE8F5E9)
private val GreenBorder = Color(0xFF4CAF50).copy(alpha = 0.5f)

// Green pill chip for in-game contextual prompts (robber move, road building, notices).
@Composable
fun NoticeChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = Shapes.pill,
        color = GreenContainer,
        contentColor = GreenOnContainer,
        border = BorderStroke(1.dp, GreenBorder),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
