package eric.bitria.hexonkmp.ui.theme

import androidx.compose.ui.graphics.Color
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.Resource

// All game-specific color / icon / label mappings in one place, parallel to the
// Material3 ColorScheme. Components import from here rather than defining their
// own per-type visuals so every palette can be tuned in a single file.

object ResourcePalette {
    fun color(resource: Resource): Color = when (resource) {
        Resource.BRICK  -> Color(0xFFB85533)  // clay red
        Resource.LUMBER -> Color(0xFF2E6B30)  // forest green
        Resource.WOOL   -> Color(0xFF8DC75C)  // pasture green
        Resource.GRAIN  -> Color(0xFFE6C247)  // wheat gold
        Resource.ORE    -> Color(0xFF73787F)  // mountain grey
    }

    fun onColor(resource: Resource): Color = when (resource) {
        Resource.WOOL, Resource.GRAIN -> Color(0xFF1A1A1A) // light bg → dark content
        else -> Color.White
    }

    fun label(resource: Resource): String = when (resource) {
        Resource.BRICK  -> "Brick"
        Resource.LUMBER -> "Lumber"
        Resource.WOOL   -> "Wool"
        Resource.GRAIN  -> "Grain"
        Resource.ORE    -> "Ore"
    }

    fun icon(resource: Resource): String = when (resource) {
        Resource.BRICK  -> "files/icons/svg/ic_brick.svg"
        Resource.LUMBER -> "files/icons/svg/ic_lumber.svg"
        Resource.WOOL   -> "files/icons/svg/ic_wool.svg"
        Resource.GRAIN  -> "files/icons/svg/ic_grain.svg"
        Resource.ORE    -> "files/icons/svg/ic_ore.svg"
    }
}

object DevCardPalette {
    fun color(card: DevCard): Color = when (card) {
        DevCard.KNIGHT         -> Color(0xFFB23A48)
        DevCard.VICTORY_POINT  -> Color(0xFFD4A017)
        DevCard.ROAD_BUILDING  -> Color(0xFF8D6E63)
        DevCard.YEAR_OF_PLENTY -> Color(0xFF4C9A5B)
        DevCard.MONOPOLY       -> Color(0xFF7E57C2)
    }

    fun icon(card: DevCard): String = when (card) {
        DevCard.KNIGHT         -> "files/icons/svg/ic_dev_knight.svg"
        DevCard.VICTORY_POINT  -> "files/icons/svg/ic_dev_vp.svg"
        DevCard.ROAD_BUILDING  -> "files/icons/svg/ic_road.svg"
        DevCard.YEAR_OF_PLENTY -> "files/icons/svg/ic_dev_year_of_plenty.svg"
        DevCard.MONOPOLY       -> "files/icons/svg/ic_dev_monopoly.svg"
    }

    fun label(card: DevCard): String = when (card) {
        DevCard.KNIGHT         -> "Knight"
        DevCard.VICTORY_POINT  -> "Victory Point"
        DevCard.ROAD_BUILDING  -> "Road Building"
        DevCard.YEAR_OF_PLENTY -> "Year of Plenty"
        DevCard.MONOPOLY       -> "Monopoly"
    }
}

object PlayerPalette {
    private val colors = listOf(
        Color(0xFFD93333), // red
        Color(0xFF3373D9), // blue
        Color(0xFFF2F2F2), // white
        Color(0xFFF28C26), // orange
    )

    fun color(player: PlayerId, players: List<PlayerId>): Color =
        colors[players.indexOf(player).coerceAtLeast(0) % colors.size]

    fun label(player: PlayerId, players: List<PlayerId>, me: PlayerId): String =
        if (player == me) "You" else "P${players.indexOf(player).coerceAtLeast(0) + 1}"
}
