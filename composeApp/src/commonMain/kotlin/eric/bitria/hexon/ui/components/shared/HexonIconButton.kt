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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object HexonIconButton {

    @Composable
    fun Transparent(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        backgroundColor: Color = Color.Transparent,
        modifier: Modifier = Modifier
    ) {
        Base(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            backgroundColor = backgroundColor,
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
            modifier = modifier
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
            tint = MaterialTheme.colorScheme.onSecondary,
            backgroundColor = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )
    }

    @Composable
    fun Tertiary(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        modifier: Modifier = Modifier
    ) {
        Base(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onTertiary,
            backgroundColor = MaterialTheme.colorScheme.tertiary,
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
                .size(48.dp)
                .clip(CircleShape)
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
