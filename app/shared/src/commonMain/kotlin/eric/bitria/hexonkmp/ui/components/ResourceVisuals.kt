package eric.bitria.hexonkmp.ui.components

import androidx.compose.ui.graphics.Color
import eric.bitria.hexonkmp.core.game.model.board.Resource

// UI (Compose) presentation for resources: card colors + short labels. Kept
// separate from the board's Filament colors (ResourceColors) since these are
// androidx Compose colors for the 2D HUD. Matches the board palette in spirit.
object ResourceVisuals {

    fun color(resource: Resource): Color = when (resource) {
        Resource.BRICK -> Color(0xFFB85533)   // clay red
        Resource.LUMBER -> Color(0xFF2E6B30)  // forest green
        Resource.WOOL -> Color(0xFF8DC75C)    // pasture green
        Resource.GRAIN -> Color(0xFFE6C247)   // wheat gold
        Resource.ORE -> Color(0xFF73787F)     // mountain grey
    }

    // A readable on-color (text/icon) for the given resource card background.
    fun onColor(resource: Resource): Color = when (resource) {
        Resource.WOOL, Resource.GRAIN -> Color(0xFF1A1A1A) // light bg -> dark text
        else -> Color.White
    }

    fun label(resource: Resource): String = when (resource) {
        Resource.BRICK -> "Brick"
        Resource.LUMBER -> "Lumber"
        Resource.WOOL -> "Wool"
        Resource.GRAIN -> "Grain"
        Resource.ORE -> "Ore"
    }
}
