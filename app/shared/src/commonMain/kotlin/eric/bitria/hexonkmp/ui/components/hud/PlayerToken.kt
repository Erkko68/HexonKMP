package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// A player's avatar: colored rounded token with a person icon.
// [color] and [label] are pre-resolved by the caller via PlayerPalette.
@Composable
fun PlayerToken(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = Tokens.tokenMd,
) {
    val onColor = if (color.luminance() > 0.6f) Color.Black else Color.White
    Card(
        modifier = modifier.size(size),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = color, contentColor = onColor),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Person,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(0.6f),
            )
        }
    }
}
