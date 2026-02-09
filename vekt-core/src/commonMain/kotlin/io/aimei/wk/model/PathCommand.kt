package io.aimei.wk.model

/**
 * SVG Path command intermediate representation
 * These are platform-agnostic path commands that can be converted to any graphics API
 */
sealed class PathCommand {
    /**
     * Move to absolute position
     */
    data class MoveTo(val x: Float, val y: Float) : PathCommand()

    /**
     * Line to absolute position
     */
    data class LineTo(val x: Float, val y: Float) : PathCommand()

    /**
     * Cubic Bezier curve to absolute position
     */
    data class CubicTo(
        val x1: Float, val y1: Float,  // First control point
        val x2: Float, val y2: Float,  // Second control point
        val x: Float, val y: Float     // End point
    ) : PathCommand()

    /**
     * Quadratic Bezier curve to absolute position
     */
    data class QuadTo(
        val x1: Float, val y1: Float,  // Control point
        val x: Float, val y: Float     // End point
    ) : PathCommand()

    /**
     * Close the current subpath
     */
    data object Close : PathCommand()
}

/**
 * A list of path commands representing a complete path
 */
data class PathCommands(
    val commands: List<PathCommand>
) {
    companion object {
        val Empty = PathCommands(emptyList())
    }
}
