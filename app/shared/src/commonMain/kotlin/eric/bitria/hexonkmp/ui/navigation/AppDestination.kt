package eric.bitria.hexonkmp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    data object Game : AppDestination("Game", Icons.Default.PlayArrow)
    data object Settings : AppDestination("Settings", Icons.Default.Settings)

    companion object {
        val all: List<AppDestination> get() = listOf(Game, Settings)
    }
}
