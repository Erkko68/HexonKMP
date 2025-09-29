package eric.bitria.hexon.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Color Definitions ---
val Blue500 = Color(0xFF1193D4)  // primary color (#1193d4)
val Blue600 = Color(0xFF0F87C1)  // darker blue (#0f87c1)
val Blue400 = Color(0xFF13A0E8)  // lighter blue (#13a0e8)

val Gray900 = Color(0xFF111618)  // dark background (#111618)
val Gray800 = Color(0xFF1A1F23)  // slightly lighter surface
val Gray700 = Color(0xFF2C3E44)  // borders / outlines
val Gray400 = Color(0xFF9DB0B9)  // text-gray
val Gray200 = Color(0xFFE5E7EB)  // light gray
val Gray100 = Color(0xFFF3F4F6)  // very light gray
val White = Color(0xFFFFFFFF)    // text-white
val Black = Color(0xFF000000)    // scrim
val Red500 = Color(0xFFEF4444)   // error

// --- Dark Mode Color Scheme ---
val HexonDarkColors: ColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray700,
    onPrimaryContainer = Gray400,

    secondary = Blue500,
    onSecondary = White,
    secondaryContainer = Blue500,
    onSecondaryContainer = White,

    tertiary = Gray800,
    onTertiary = Gray400,
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

    scrim = Black,

    inverseSurface = White,
    inverseOnSurface = Gray900,
    inversePrimary = Blue500,

    surfaceDim = Gray900,
    surfaceBright = Gray800,
    surfaceContainerLowest = Gray900,
    surfaceContainerLow = Gray800,
    surfaceContainer = Gray800,
    surfaceContainerHigh = Gray700,
    surfaceContainerHighest = Gray700,

    surfaceTint = Blue500
)

// --- Light Mode Color Scheme ---
val HexonLightColors: ColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = White,
    primaryContainer = Blue500,
    onPrimaryContainer = White,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray400,
    onSecondaryContainer = Gray900,

    tertiary = Blue500,
    onTertiary = White,
    tertiaryContainer = Blue400,
    onTertiaryContainer = White,

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

    scrim = Black,

    inverseSurface = Gray900,
    inverseOnSurface = White,
    inversePrimary = Blue500,

    surfaceDim = Gray200,
    surfaceBright = White,
    surfaceContainerLowest = White,
    surfaceContainerLow = Gray100,
    surfaceContainer = Gray100,
    surfaceContainerHigh = Gray200,
    surfaceContainerHighest = Gray400,

    surfaceTint = Blue600
)
