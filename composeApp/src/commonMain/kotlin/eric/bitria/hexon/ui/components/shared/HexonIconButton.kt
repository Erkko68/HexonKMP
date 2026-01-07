package eric.bitria.hexon.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object HexonIconButton {

    @Composable
    fun Transparent(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        modifier: Modifier = Modifier
    ) {
        Base(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            backgroundColor = Color.Transparent,
            modifier = modifier
        )
    }

    @Composable
    fun Primary(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        modifier: Modifier = Modifier
    ) {
        Base(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            backgroundColor = MaterialTheme.colorScheme.primary,
            modifier = modifier.shadow(4.dp, CircleShape)
        )
    }

    @Composable
    fun Secondary(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        modifier: Modifier = Modifier
    ) {
        Base(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = modifier
        )
    }

    @Composable
    private fun Base(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String?,
        tint: Color,
        backgroundColor: Color,
        modifier: Modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .clip(CircleShape)
                .size(48.dp)
                .background(backgroundColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}
