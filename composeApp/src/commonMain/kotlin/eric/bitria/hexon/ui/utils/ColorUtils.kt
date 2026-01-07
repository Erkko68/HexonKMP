package eric.bitria.hexon.ui.utils

import androidx.compose.ui.graphics.Color

private val vividColors = listOf(
    Color(0xFFEF4444), // Red
    Color(0xFFF97316), // Orange
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF3B82F6), // Blue
    Color(0xFF6366F1), // Indigo
    Color(0xFF8B5CF6), // Violet
    Color(0xFFD946EF), // Fuchsia
    Color(0xFFEC4899)  // Pink
)

fun String.toVividColor(): Color {
    if (this.isEmpty()) return vividColors[0]
    val hash = this.hashCode()
    val index = (if (hash < 0) -hash else hash) % vividColors.size
    return vividColors[index]
}
