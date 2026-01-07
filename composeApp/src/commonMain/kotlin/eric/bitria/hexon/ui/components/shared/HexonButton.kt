package eric.bitria.hexon.ui.components.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Global Button configuration to ensure UI consistency.
 */
object HexonButtonDefaults {
    val Height = 56.dp
    val CornerRadius = 16.dp
    val ShadowElevation = 4.dp
    val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
}

@Composable
fun HexonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    paddingScale: Dp? = null,
    content: @Composable (() -> Unit)? = null
) {
    val height = paddingScale?.let { it * 0.12f } ?: HexonButtonDefaults.Height
    val shape = paddingScale?.let { RoundedCornerShape(it * 0.03f) } ?: RoundedCornerShape(HexonButtonDefaults.CornerRadius)
    val fontSize = paddingScale?.let { (it * 0.045f).value.sp } ?: 16.sp

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .shadow(HexonButtonDefaults.ShadowElevation, shape),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        ),
        contentPadding = HexonButtonDefaults.ContentPadding
    ) {
        if (content != null) {
            content()
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
