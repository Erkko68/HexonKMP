package eric.bitria.hexonkmp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Shape tokens shared across the whole UI, analogous to Spacing.
// Use these instead of hardcoding RoundedCornerShape(12.dp) per-component so the
// look can be tuned in one place.
object Shapes {
    val card = RoundedCornerShape(12.dp)  // standard card/token corner radius
    val pill = RoundedCornerShape(percent = 50)  // phase pill, badges
    val selectedBorder = 3.dp  // selection ring (resource card active state)
    val activeBorder = 2.dp    // lighter active ring (playable dev card)
}
