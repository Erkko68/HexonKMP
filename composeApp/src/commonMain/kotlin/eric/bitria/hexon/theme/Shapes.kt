package eric.bitria.hexon.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified spacing system based on the dynamic paddingScale.
 */
data class HexonSpacing(
    val extraSmall: Dp = 0.dp,   // 0.01f
    val small: Dp = 0.dp,        // 0.02f
    val mediumSmall: Dp = 0.dp,  // 0.03f
    val medium: Dp = 0.dp,       // 0.04f
    val mediumLarge: Dp = 0.dp,  // 0.05f
    val large: Dp = 0.dp,        // 0.06f
    val extraLarge: Dp = 0.dp,   // 0.08f
    
    // Screen defaults
    val screenHorizontal: Dp = 0.dp,
    val screenVertical: Dp = 0.dp
)

/**
 * Unified shape system based on the dynamic paddingScale.
 */
data class HexonShapes(
    val small: Shape = RoundedCornerShape(0.dp),  // 0.015f
    val medium: Shape = RoundedCornerShape(0.dp), // 0.02f
    val large: Shape = RoundedCornerShape(0.dp)   // 0.03f
)

data class HexonDimensions(
    val paddingScale: Dp,
    val spacing: HexonSpacing,
    val shapes: HexonShapes,
    val listItemHeight: Dp // New unified height
)

val LocalHexonDimensions = staticCompositionLocalOf<HexonDimensions> {
    error("No HexonDimensions provided")
}

object HexonTheme {
    val dimensions: HexonDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalHexonDimensions.current
}

fun createHexonDimensions(paddingScale: Dp): HexonDimensions {
    return HexonDimensions(
        paddingScale = paddingScale,
        spacing = HexonSpacing(
            extraSmall = paddingScale * 0.01f,
            small = paddingScale * 0.02f,
            mediumSmall = paddingScale * 0.03f,
            medium = paddingScale * 0.04f,
            mediumLarge = paddingScale * 0.05f,
            large = paddingScale * 0.06f,
            extraLarge = paddingScale * 0.08f,
            screenHorizontal = paddingScale * 0.04f,
            screenVertical = paddingScale * 0.02f
        ),
        shapes = HexonShapes(
            small = RoundedCornerShape(paddingScale * 0.015f),
            medium = RoundedCornerShape(paddingScale * 0.02f),
            large = RoundedCornerShape(paddingScale * 0.03f)
        ),
        listItemHeight = paddingScale * 0.15f // Increased from 0.12f for better balance
    )
}

val HexonM3Shapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
