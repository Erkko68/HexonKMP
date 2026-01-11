package eric.bitria.hexon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

@Composable
fun HexonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = if (darkTheme) HexonDarkColors else HexonLightColors,
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        // Calculate dynamic padding scale based on screen size
        val paddingScale = if (maxWidth == 0.dp || maxHeight == 0.dp) 400.dp else minOf(maxWidth, maxHeight)
        
        val dimensions = createHexonDimensions(paddingScale)
        val typography = createHexonTypography(paddingScale)

        CompositionLocalProvider(
            LocalHexonDimensions provides dimensions
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = HexonM3Shapes,
                content = content
            )
        }
    }
}
