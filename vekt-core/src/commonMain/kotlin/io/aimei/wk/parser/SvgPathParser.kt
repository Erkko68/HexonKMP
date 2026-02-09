package io.aimei.wk.parser

import io.aimei.wk.model.PathCommand
import io.aimei.wk.model.PathCommands
import kotlin.math.*

/**
 * SVG Path command parser
 *
 * Parses SVG path "d" attribute into platform-agnostic PathCommands.
 *
 * Supported SVG commands:
 * - M/m: moveTo
 * - L/l: lineTo
 * - H/h: horizontal lineTo
 * - V/v: vertical lineTo
 * - C/c: cubicTo (cubic Bezier curve)
 * - S/s: smooth cubicTo
 * - Q/q: quadTo (quadratic Bezier curve)
 * - T/t: smooth quadTo
 * - A/a: elliptical arc (converted to Bezier curves)
 * - Z/z: close path
 */
object SvgPathParser {

    /**
     * Parse SVG path data string into PathCommands
     *
     * @param pathData SVG path "d" attribute value
     * @param scale Scale factor for coordinates
     * @param offsetX X offset for coordinates
     * @param offsetY Y offset for coordinates
     * @return PathCommands containing list of path commands
     */
    fun parse(
        pathData: String,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): PathCommands {
        val commands = mutableListOf<PathCommand>()
        val tokens = tokenize(pathData)
        var i = 0

        // Current position
        var currentX = 0f
        var currentY = 0f
        // Subpath start (for Z command)
        var startX = 0f
        var startY = 0f
        // Last control point for S/s commands
        var lastCubicControlX = 0f
        var lastCubicControlY = 0f
        // Last control point for T/t commands
        var lastQuadControlX = 0f
        var lastQuadControlY = 0f
        // Last command type
        var lastCommand = ' '

        while (i < tokens.size) {
            val token = tokens[i]

            when (token) {
                "M" -> {
                    i++
                    var isFirst = true
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        if (isFirst) {
                            commands.add(PathCommand.MoveTo(x, y))
                            startX = x
                            startY = y
                            isFirst = false
                        } else {
                            commands.add(PathCommand.LineTo(x, y))
                        }
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'M'
                }

                "m" -> {
                    i++
                    var isFirst = true
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        if (isFirst) {
                            currentX += dx
                            currentY += dy
                            commands.add(PathCommand.MoveTo(currentX, currentY))
                            startX = currentX
                            startY = currentY
                            isFirst = false
                        } else {
                            currentX += dx
                            currentY += dy
                            commands.add(PathCommand.LineTo(currentX, currentY))
                        }
                    }
                    lastCommand = 'm'
                }

                "L" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.LineTo(x, y))
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'L'
                }

                "l" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        currentX += dx
                        currentY += dy
                        commands.add(PathCommand.LineTo(currentX, currentY))
                    }
                    lastCommand = 'l'
                }

                "H" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x = tokens[i++].toFloat() * scale + offsetX
                        commands.add(PathCommand.LineTo(x, currentY))
                        currentX = x
                    }
                    lastCommand = 'H'
                }

                "h" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dx = tokens[i++].toFloat() * scale
                        currentX += dx
                        commands.add(PathCommand.LineTo(currentX, currentY))
                    }
                    lastCommand = 'h'
                }

                "V" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.LineTo(currentX, y))
                        currentY = y
                    }
                    lastCommand = 'V'
                }

                "v" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dy = tokens[i++].toFloat() * scale
                        currentY += dy
                        commands.add(PathCommand.LineTo(currentX, currentY))
                    }
                    lastCommand = 'v'
                }

                "C" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x1 = tokens[i++].toFloat() * scale + offsetX
                        val y1 = tokens[i++].toFloat() * scale + offsetY
                        val x2 = tokens[i++].toFloat() * scale + offsetX
                        val y2 = tokens[i++].toFloat() * scale + offsetY
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.CubicTo(x1, y1, x2, y2, x, y))
                        lastCubicControlX = x2
                        lastCubicControlY = y2
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'C'
                }

                "c" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dx1 = tokens[i++].toFloat() * scale
                        val dy1 = tokens[i++].toFloat() * scale
                        val dx2 = tokens[i++].toFloat() * scale
                        val dy2 = tokens[i++].toFloat() * scale
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        val x2 = currentX + dx2
                        val y2 = currentY + dy2
                        commands.add(PathCommand.CubicTo(
                            currentX + dx1, currentY + dy1,
                            x2, y2,
                            currentX + dx, currentY + dy
                        ))
                        lastCubicControlX = x2
                        lastCubicControlY = y2
                        currentX += dx
                        currentY += dy
                    }
                    lastCommand = 'c'
                }

                "S" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        // First control point is reflection of last control point
                        val x1 = if (lastCommand in "CcSs") {
                            2 * currentX - lastCubicControlX
                        } else currentX
                        val y1 = if (lastCommand in "CcSs") {
                            2 * currentY - lastCubicControlY
                        } else currentY
                        val x2 = tokens[i++].toFloat() * scale + offsetX
                        val y2 = tokens[i++].toFloat() * scale + offsetY
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.CubicTo(x1, y1, x2, y2, x, y))
                        lastCubicControlX = x2
                        lastCubicControlY = y2
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'S'
                }

                "s" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x1 = if (lastCommand in "CcSs") {
                            2 * currentX - lastCubicControlX
                        } else currentX
                        val y1 = if (lastCommand in "CcSs") {
                            2 * currentY - lastCubicControlY
                        } else currentY
                        val dx2 = tokens[i++].toFloat() * scale
                        val dy2 = tokens[i++].toFloat() * scale
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        val x2 = currentX + dx2
                        val y2 = currentY + dy2
                        commands.add(PathCommand.CubicTo(x1, y1, x2, y2, currentX + dx, currentY + dy))
                        lastCubicControlX = x2
                        lastCubicControlY = y2
                        currentX += dx
                        currentY += dy
                    }
                    lastCommand = 's'
                }

                "Q" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x1 = tokens[i++].toFloat() * scale + offsetX
                        val y1 = tokens[i++].toFloat() * scale + offsetY
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.QuadTo(x1, y1, x, y))
                        lastQuadControlX = x1
                        lastQuadControlY = y1
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'Q'
                }

                "q" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val dx1 = tokens[i++].toFloat() * scale
                        val dy1 = tokens[i++].toFloat() * scale
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        val x1 = currentX + dx1
                        val y1 = currentY + dy1
                        commands.add(PathCommand.QuadTo(x1, y1, currentX + dx, currentY + dy))
                        lastQuadControlX = x1
                        lastQuadControlY = y1
                        currentX += dx
                        currentY += dy
                    }
                    lastCommand = 'q'
                }

                "T" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x1 = if (lastCommand in "QqTt") {
                            2 * currentX - lastQuadControlX
                        } else currentX
                        val y1 = if (lastCommand in "QqTt") {
                            2 * currentY - lastQuadControlY
                        } else currentY
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        commands.add(PathCommand.QuadTo(x1, y1, x, y))
                        lastQuadControlX = x1
                        lastQuadControlY = y1
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'T'
                }

                "t" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val x1 = if (lastCommand in "QqTt") {
                            2 * currentX - lastQuadControlX
                        } else currentX
                        val y1 = if (lastCommand in "QqTt") {
                            2 * currentY - lastQuadControlY
                        } else currentY
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        commands.add(PathCommand.QuadTo(x1, y1, currentX + dx, currentY + dy))
                        lastQuadControlX = x1
                        lastQuadControlY = y1
                        currentX += dx
                        currentY += dy
                    }
                    lastCommand = 't'
                }

                "A" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val rx = tokens[i++].toFloat() * scale
                        val ry = tokens[i++].toFloat() * scale
                        val xAxisRotation = tokens[i++].toFloat()
                        val largeArcFlag = tokens[i++].toFloat().toInt() != 0
                        val sweepFlag = tokens[i++].toFloat().toInt() != 0
                        val x = tokens[i++].toFloat() * scale + offsetX
                        val y = tokens[i++].toFloat() * scale + offsetY
                        arcToCubicBezier(commands, currentX, currentY, x, y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag)
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'A'
                }

                "a" -> {
                    i++
                    while (i < tokens.size && isNumber(tokens[i])) {
                        val rx = tokens[i++].toFloat() * scale
                        val ry = tokens[i++].toFloat() * scale
                        val xAxisRotation = tokens[i++].toFloat()
                        val largeArcFlag = tokens[i++].toFloat().toInt() != 0
                        val sweepFlag = tokens[i++].toFloat().toInt() != 0
                        val dx = tokens[i++].toFloat() * scale
                        val dy = tokens[i++].toFloat() * scale
                        val x = currentX + dx
                        val y = currentY + dy
                        arcToCubicBezier(commands, currentX, currentY, x, y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag)
                        currentX = x
                        currentY = y
                    }
                    lastCommand = 'a'
                }

                "Z", "z" -> {
                    commands.add(PathCommand.Close)
                    currentX = startX
                    currentY = startY
                    i++
                    lastCommand = 'Z'
                }

                else -> {
                    // Skip unknown tokens
                    i++
                }
            }
        }

        return PathCommands(commands)
    }

    /**
     * Convert elliptical arc to cubic Bezier curves
     */
    private fun arcToCubicBezier(
        commands: MutableList<PathCommand>,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        rx: Float, ry: Float,
        xAxisRotation: Float,
        largeArcFlag: Boolean,
        sweepFlag: Boolean
    ) {
        // Handle degenerate cases
        if (rx == 0f || ry == 0f) {
            commands.add(PathCommand.LineTo(x2, y2))
            return
        }
        if (x1 == x2 && y1 == y2) return

        var rxAbs = abs(rx)
        var ryAbs = abs(ry)

        // Convert angle to radians
        val phi = xAxisRotation * PI.toFloat() / 180f
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)

        // Step 1: Compute (x1', y1')
        val dx = (x1 - x2) / 2f
        val dy = (y1 - y2) / 2f
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy

        // Correct radii if too small
        val lambda = (x1p * x1p) / (rxAbs * rxAbs) + (y1p * y1p) / (ryAbs * ryAbs)
        if (lambda > 1f) {
            val sqrtLambda = sqrt(lambda)
            rxAbs *= sqrtLambda
            ryAbs *= sqrtLambda
        }

        // Step 2: Compute (cx', cy')
        val rxSq = rxAbs * rxAbs
        val rySq = ryAbs * ryAbs
        val x1pSq = x1p * x1p
        val y1pSq = y1p * y1p

        var sq = ((rxSq * rySq) - (rxSq * y1pSq) - (rySq * x1pSq)) /
                ((rxSq * y1pSq) + (rySq * x1pSq))
        if (sq < 0f) sq = 0f
        var coef = sqrt(sq)
        if (largeArcFlag == sweepFlag) coef = -coef

        val cxp = coef * rxAbs * y1p / ryAbs
        val cyp = -coef * ryAbs * x1p / rxAbs

        // Step 3: Compute (cx, cy)
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        val cx = cosPhi * cxp - sinPhi * cyp + mx
        val cy = sinPhi * cxp + cosPhi * cyp + my

        // Step 4: Compute theta1 and dtheta
        fun angle(ux: Float, uy: Float, vx: Float, vy: Float): Float {
            val dot = ux * vx + uy * vy
            val len = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
            var ang = acos((dot / len).coerceIn(-1f, 1f))
            if (ux * vy - uy * vx < 0f) ang = -ang
            return ang
        }

        val ux = (x1p - cxp) / rxAbs
        val uy = (y1p - cyp) / ryAbs
        val vx = (-x1p - cxp) / rxAbs
        val vy = (-y1p - cyp) / ryAbs

        val theta1 = angle(1f, 0f, ux, uy)
        var dtheta = angle(ux, uy, vx, vy)

        if (!sweepFlag && dtheta > 0f) {
            dtheta -= 2f * PI.toFloat()
        } else if (sweepFlag && dtheta < 0f) {
            dtheta += 2f * PI.toFloat()
        }

        // Step 5: Convert arc to Bezier curves (one segment per 90 degrees)
        val segments = ceil(abs(dtheta) / (PI.toFloat() / 2f)).toInt().coerceAtLeast(1)
        val delta = dtheta / segments

        for (seg in 0 until segments) {
            val t1 = theta1 + seg * delta
            val t2 = t1 + delta

            // Bezier control point calculation
            val alpha = sin(delta) * (sqrt(4f + 3f * tan(delta / 2f).let { it * it }) - 1f) / 3f

            val cos1 = cos(t1)
            val sin1 = sin(t1)
            val cos2 = cos(t2)
            val sin2 = sin(t2)

            // Points on ellipse
            val ex1 = rxAbs * cos1
            val ey1 = ryAbs * sin1
            val ex2 = rxAbs * cos2
            val ey2 = ryAbs * sin2

            // Derivatives
            val dx1 = -rxAbs * sin1
            val dy1 = ryAbs * cos1
            val dx2 = -rxAbs * sin2
            val dy2 = ryAbs * cos2

            // Control points in ellipse space
            val cpx1 = ex1 + alpha * dx1
            val cpy1 = ey1 + alpha * dy1
            val cpx2 = ex2 - alpha * dx2
            val cpy2 = ey2 - alpha * dy2

            // Transform back to original coordinate system
            fun transform(px: Float, py: Float): Pair<Float, Float> {
                return Pair(
                    cosPhi * px - sinPhi * py + cx,
                    sinPhi * px + cosPhi * py + cy
                )
            }

            val (bx1, by1) = transform(cpx1, cpy1)
            val (bx2, by2) = transform(cpx2, cpy2)
            val (endX, endY) = transform(ex2, ey2)

            commands.add(PathCommand.CubicTo(bx1, by1, bx2, by2, endX, endY))
        }
    }

    /**
     * Tokenize SVG path data string
     */
    private fun tokenize(pathData: String): List<String> {
        val result = mutableListOf<String>()
        val commands = setOf(
            'M', 'm', 'L', 'l', 'H', 'h', 'V', 'v',
            'C', 'c', 'S', 's', 'Q', 'q', 'T', 't',
            'A', 'a', 'Z', 'z'
        )

        var i = 0
        while (i < pathData.length) {
            val c = pathData[i]

            when {
                c in commands -> {
                    result.add(c.toString())
                    i++
                }

                c == '-' || c == '+' || c == '.' || c.isDigit() -> {
                    val start = i
                    if (c == '-' || c == '+') i++
                    // Track if we've seen a decimal point (only one allowed per number)
                    var hasDecimal = false
                    while (i < pathData.length) {
                        val ch = pathData[i]
                        when {
                            ch.isDigit() -> i++
                            ch == '.' && !hasDecimal -> {
                                hasDecimal = true
                                i++
                            }
                            else -> break
                        }
                    }
                    // Handle scientific notation
                    if (i < pathData.length && (pathData[i] == 'e' || pathData[i] == 'E')) {
                        i++
                        if (i < pathData.length && (pathData[i] == '-' || pathData[i] == '+')) i++
                        while (i < pathData.length && pathData[i].isDigit()) i++
                    }
                    result.add(pathData.substring(start, i))
                }

                c == ',' || c.isWhitespace() -> {
                    i++
                }

                else -> {
                    i++
                }
            }
        }
        return result
    }

    private fun isNumber(token: String): Boolean {
        return token.firstOrNull()?.let { it.isDigit() || it == '-' || it == '+' || it == '.' }
            ?: false
    }
}
