package eric.bitria.hexonkmp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Dark-only scheme. The game is always fullscreen with a 3D board, so a light
// theme makes no sense. Colors are chosen to feel warm and board-game-adjacent:
// amber primary, near-black surfaces, readable contrast on every surface.
val HexonColorScheme = darkColorScheme(
    primary             = Color(0xFFE8A917), // amber — primary actions, active states
    onPrimary           = Color(0xFF1A1000),
    primaryContainer    = Color(0xFF3A2800), // dark amber container (roll badge)
    onPrimaryContainer  = Color(0xFFFFDF9E),

    secondary           = Color(0xFF7E92B5), // steel blue — secondary UI
    onSecondary         = Color(0xFF0A1929),
    secondaryContainer  = Color(0xFF1E2D3D),
    onSecondaryContainer = Color(0xFFB8CCE8),

    background          = Color(0xFF0F0F0F), // near-black (behind the board)
    onBackground        = Color(0xFFE6E1E5),

    surface             = Color(0xFF1A1A1A), // HUD cards, sheets
    onSurface           = Color(0xFFE6E1E5),
    surfaceVariant      = Color(0xFF2A2A2A), // unselected action buttons
    onSurfaceVariant    = Color(0xFFCAC4D0),

    error               = Color(0xFFCF6679),
    onError             = Color(0xFF370012),
    errorContainer      = Color(0xFF490020), // 7-roll badge background
    onErrorContainer    = Color(0xFFFFB3BC),

    outline             = Color(0xFF403F46),
    scrim               = Color(0xFF000000),
)
