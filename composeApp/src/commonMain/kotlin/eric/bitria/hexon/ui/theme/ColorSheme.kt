package eric.bitria.hexon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Simplified Color Palette ---
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

val Gray900 = Color(0xFF111618)  // deep background
val Gray800 = Color(0xFF1A1F23)  // surface
val Gray700 = Color(0xFF2C3E44)  // surface variant / borders
val Gray400 = Color(0xFF9DB0B9)  // secondary text
val Gray200 = Color(0xFFE5E7EB)  // light surface variant
val Gray100 = Color(0xFFF3F4F6)  // very light background

val Red500 = Color(0xFFEF4444)   // error

// --- Dark Mode Color Scheme ---
val HexonDarkColors: ColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = White,

    secondary = Gray400,
    onSecondary = Black,
    secondaryContainer = Gray700,
    onSecondaryContainer = White,

    tertiary = Gray700,
    onTertiary = White,
    tertiaryContainer = Gray800,
    onTertiaryContainer = Gray400,

    background = Gray900,
    onBackground = Gray400,

    surface = Gray800,
    onSurface = White,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray400,

    error = Red500,
    onError = White,
    errorContainer = Red500.copy(alpha = 0.2f),
    onErrorContainer = Red500,

    outline = Gray700,
    outlineVariant = Gray400,

    scrim = Black
)

// --- Light Mode Color Scheme ---
val HexonLightColors: ColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray200,
    onPrimaryContainer = Black,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Black,

    tertiary = Gray400,
    onTertiary = Black,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Black,

    background = White,
    onBackground = Gray900,

    surface = Gray100,
    onSurface = Gray900,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray700,

    error = Red500,
    onError = White,
    errorContainer = Red500.copy(alpha = 0.2f),
    onErrorContainer = Red500,

    outline = Gray400,
    outlineVariant = Gray200,

    scrim = Black
)
