package io.aimei.wk.parser

import io.aimei.wk.model.Transform
import kotlin.math.PI

/**
 * Parser for SVG transform attribute
 * Supports: translate, scale, rotate, skewX, skewY, matrix
 */
object TransformParser {

    /**
     * Parse transform attribute value into Transform model
     * Examples:
     * - "translate(10, 20)"
     * - "scale(2)"
     * - "rotate(45 50 50)"
     * - "matrix(1 0 0 1 0 0)"
     * - "translate(10 20) rotate(45)"
     */
    fun parse(transformAttr: String): Transform? {
        val transforms = mutableListOf<Transform>()
        val regex = Regex("""(\w+)\s*\(([^)]+)\)""")

        regex.findAll(transformAttr).forEach { match ->
            val type = match.groupValues[1].lowercase()
            val params = parseParams(match.groupValues[2])

            val transform = when (type) {
                "translate" -> parseTranslate(params)
                "scale" -> parseScale(params)
                "rotate" -> parseRotate(params)
                "skewx" -> parseSkewX(params)
                "skewy" -> parseSkewY(params)
                "matrix" -> parseMatrix(params)
                else -> null
            }

            transform?.let { transforms.add(it) }
        }

        return when {
            transforms.isEmpty() -> null
            transforms.size == 1 -> transforms.first()
            else -> Transform.Combined(transforms)
        }
    }

    /**
     * Parse comma/space separated parameters
     */
    private fun parseParams(paramsStr: String): List<Float> {
        return paramsStr.trim()
            .split(Regex("[\\s,]+"))
            .mapNotNull { it.toFloatOrNull() }
    }

    /**
     * Parse translate(tx [, ty])
     */
    private fun parseTranslate(params: List<Float>): Transform? {
        if (params.isEmpty()) return null
        val tx = params[0]
        val ty = params.getOrElse(1) { 0f }
        return Transform.Translate(tx, ty)
    }

    /**
     * Parse scale(sx [, sy])
     */
    private fun parseScale(params: List<Float>): Transform? {
        if (params.isEmpty()) return null
        val sx = params[0]
        val sy = params.getOrElse(1) { sx }
        return Transform.Scale(sx, sy)
    }

    /**
     * Parse rotate(angle [, cx, cy])
     */
    private fun parseRotate(params: List<Float>): Transform? {
        if (params.isEmpty()) return null
        val angle = params[0]
        val cx = params.getOrElse(1) { 0f }
        val cy = params.getOrElse(2) { 0f }
        return Transform.Rotate(angle, cx, cy)
    }

    /**
     * Parse skewX(angle)
     */
    private fun parseSkewX(params: List<Float>): Transform? {
        if (params.isEmpty()) return null
        return Transform.SkewX(params[0])
    }

    /**
     * Parse skewY(angle)
     */
    private fun parseSkewY(params: List<Float>): Transform? {
        if (params.isEmpty()) return null
        return Transform.SkewY(params[0])
    }

    /**
     * Parse matrix(a, b, c, d, e, f)
     */
    private fun parseMatrix(params: List<Float>): Transform? {
        if (params.size < 6) return null
        return Transform.Matrix(
            a = params[0],
            b = params[1],
            c = params[2],
            d = params[3],
            e = params[4],
            f = params[5]
        )
    }

    /**
     * Convert degrees to radians
     */
    fun degreesToRadians(degrees: Float): Float {
        return degrees * (PI.toFloat() / 180f)
    }
}
