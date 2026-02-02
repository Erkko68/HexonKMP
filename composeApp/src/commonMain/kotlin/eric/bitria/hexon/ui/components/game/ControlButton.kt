package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import eric.bitria.hexon.ui.theme.HexonTheme

@Composable
fun ControlButton(
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shapes = HexonTheme.dimensions.shapes
    val spacing = HexonTheme.dimensions.spacing

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = spacing.extraSmall,
                shape = shapes.medium,
                clip = false
            )
            .clip(shapes.medium)
            .background(color)
            .border(
                width = spacing.extraSmall * 0.5f,
                color = Color.White.copy(alpha = 0.2f),
                shape = shapes.medium
            )
            .clickable(
                onClick = onClick,
                enabled = enabled
            )
            .padding(spacing.small),
        contentAlignment = Alignment.Center
    ){
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}
