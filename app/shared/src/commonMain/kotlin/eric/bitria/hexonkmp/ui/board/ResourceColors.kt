package eric.bitria.hexonkmp.ui.board

import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import io.github.erkko68.filament.compose.scene.Color

// Maps game terrain/players to the baseColor fed to the hex material instance.
// Linear-ish RGB in 0..1, tuned to read as the classic Catan palette.
object ResourceColors {

    fun forTerrain(terrain: Terrain): Color = when (terrain) {
        Terrain.HILLS -> Color(0.72f, 0.33f, 0.20f)     // brick — clay red
        Terrain.FOREST -> Color(0.16f, 0.40f, 0.18f)    // lumber — dark green
        Terrain.PASTURE -> Color(0.55f, 0.78f, 0.36f)   // wool — light green
        Terrain.FIELDS -> Color(0.90f, 0.76f, 0.28f)    // grain — wheat gold
        Terrain.MOUNTAINS -> Color(0.45f, 0.48f, 0.52f) // ore — grey
        Terrain.DESERT -> Color(0.85f, 0.78f, 0.55f)    // sand
    }

    // Stable per-player color for buildings/roads, by seat order.
    fun forPlayer(player: PlayerId, players: List<PlayerId>): Color {
        val palette = listOf(
            Color(0.85f, 0.20f, 0.20f), // red
            Color(0.20f, 0.45f, 0.85f), // blue
            Color(0.95f, 0.95f, 0.95f), // white
            Color(0.95f, 0.55f, 0.15f), // orange
        )
        val i = players.indexOf(player).coerceAtLeast(0)
        return palette[i % palette.size]
    }
}
