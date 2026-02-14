package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Reusable icon button composable that maintains square aspect ratio.
 * Ideal for action buttons in trade panels and other UI components.
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    colors: IconButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(percent = 20),
        modifier = modifier.aspectRatio(1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(0.6f)
        )
    }
}

