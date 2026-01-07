package eric.bitria.hexon.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

val SpaceGrotesk = FontFamily.Default
val NotoSans = FontFamily.Default

/**
 * Creates a responsive Typography set based on the paddingScale.
 */
fun createHexonTypography(paddingScale: Dp): Typography {
    val scale = paddingScale.value
    
    return Typography(
        // Large Titles / Brand (Previous 0.08f)
        displayMedium = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.08f).sp,
            letterSpacing = 2.sp
        ),
        
        // Screen Headers (Previous 0.07f - 0.08f)
        displaySmall = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.07f).sp,
            letterSpacing = 1.sp
        ),

        // Section Headers / Large Titles
        titleLarge = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.05f).sp,
            lineHeight = (scale * 0.06f).sp
        ),

        // Primary Buttons / Important Labels (Previous 0.045f)
        titleMedium = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.045f).sp,
            lineHeight = (scale * 0.055f).sp
        ),

        // Standard Text / Input Fields / Tabs (Previous 0.04f)
        bodyLarge = TextStyle(
            fontFamily = NotoSans,
            fontWeight = FontWeight.Normal,
            fontSize = (scale * 0.04f).sp,
            lineHeight = (scale * 0.05f).sp
        ),

        // Secondary Text / Descriptive text (Previous 0.035f)
        bodyMedium = TextStyle(
            fontFamily = NotoSans,
            fontWeight = FontWeight.Normal,
            fontSize = (scale * 0.035f).sp,
            lineHeight = (scale * 0.045f).sp
        ),

        // Small Info / Support Text / Dividers (Previous 0.03f)
        bodySmall = TextStyle(
            fontFamily = NotoSans,
            fontWeight = FontWeight.Normal,
            fontSize = (scale * 0.03f).sp,
            lineHeight = (scale * 0.04f).sp
        ),

        // Labels / Small Buttons
        labelLarge = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.04f).sp
        ),
        
        labelMedium = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (scale * 0.035f).sp
        )
    )
}

// Keep the static one for legacy or non-responsive needs
val HexonTypography = Typography()
