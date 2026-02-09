package io.aimei.wk.parser

import io.aimei.wk.model.PathCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgPathParserTest {

    @Test
    fun testMoveToAbsolute() {
        val result = SvgPathParser.parse("M 10 20")
        assertEquals(1, result.commands.size)
        val cmd = result.commands[0] as PathCommand.MoveTo
        assertEquals(10f, cmd.x)
        assertEquals(20f, cmd.y)
    }

    @Test
    fun testMoveToRelative() {
        val result = SvgPathParser.parse("M 10 20 m 5 5")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.MoveTo
        assertEquals(15f, cmd.x)
        assertEquals(25f, cmd.y)
    }

    @Test
    fun testLineToAbsolute() {
        val result = SvgPathParser.parse("M 0 0 L 100 200")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(100f, cmd.x)
        assertEquals(200f, cmd.y)
    }

    @Test
    fun testLineToRelative() {
        val result = SvgPathParser.parse("M 10 10 l 20 30")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(30f, cmd.x)
        assertEquals(40f, cmd.y)
    }

    @Test
    fun testHorizontalLineAbsolute() {
        val result = SvgPathParser.parse("M 10 20 H 50")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(50f, cmd.x)
        assertEquals(20f, cmd.y)
    }

    @Test
    fun testHorizontalLineRelative() {
        val result = SvgPathParser.parse("M 10 20 h 30")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(40f, cmd.x)
        assertEquals(20f, cmd.y)
    }

    @Test
    fun testVerticalLineAbsolute() {
        val result = SvgPathParser.parse("M 10 20 V 80")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(10f, cmd.x)
        assertEquals(80f, cmd.y)
    }

    @Test
    fun testVerticalLineRelative() {
        val result = SvgPathParser.parse("M 10 20 v 30")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.LineTo
        assertEquals(10f, cmd.x)
        assertEquals(50f, cmd.y)
    }

    @Test
    fun testCubicBezierAbsolute() {
        val result = SvgPathParser.parse("M 0 0 C 10 10 20 20 30 30")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.CubicTo
        assertEquals(10f, cmd.x1)
        assertEquals(10f, cmd.y1)
        assertEquals(20f, cmd.x2)
        assertEquals(20f, cmd.y2)
        assertEquals(30f, cmd.x)
        assertEquals(30f, cmd.y)
    }

    @Test
    fun testCubicBezierRelative() {
        val result = SvgPathParser.parse("M 10 10 c 5 5 10 10 15 15")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.CubicTo
        assertEquals(15f, cmd.x1)
        assertEquals(15f, cmd.y1)
        assertEquals(20f, cmd.x2)
        assertEquals(20f, cmd.y2)
        assertEquals(25f, cmd.x)
        assertEquals(25f, cmd.y)
    }

    @Test
    fun testSmoothCubicAbsolute() {
        val result = SvgPathParser.parse("M 0 0 C 10 10 20 10 30 0 S 50 -10 60 0")
        assertEquals(3, result.commands.size)
        val cmd = result.commands[2] as PathCommand.CubicTo
        // First control point is reflection of (20, 10) around (30, 0)
        assertEquals(40f, cmd.x1)
        assertEquals(-10f, cmd.y1)
        assertEquals(50f, cmd.x2)
        assertEquals(-10f, cmd.y2)
        assertEquals(60f, cmd.x)
        assertEquals(0f, cmd.y)
    }

    @Test
    fun testQuadraticBezierAbsolute() {
        val result = SvgPathParser.parse("M 0 0 Q 50 50 100 0")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.QuadTo
        assertEquals(50f, cmd.x1)
        assertEquals(50f, cmd.y1)
        assertEquals(100f, cmd.x)
        assertEquals(0f, cmd.y)
    }

    @Test
    fun testQuadraticBezierRelative() {
        val result = SvgPathParser.parse("M 10 10 q 25 25 50 0")
        assertEquals(2, result.commands.size)
        val cmd = result.commands[1] as PathCommand.QuadTo
        assertEquals(35f, cmd.x1)
        assertEquals(35f, cmd.y1)
        assertEquals(60f, cmd.x)
        assertEquals(10f, cmd.y)
    }

    @Test
    fun testSmoothQuadraticAbsolute() {
        val result = SvgPathParser.parse("M 0 0 Q 50 50 100 0 T 200 0")
        assertEquals(3, result.commands.size)
        val cmd = result.commands[2] as PathCommand.QuadTo
        // Control point is reflection of (50, 50) around (100, 0)
        assertEquals(150f, cmd.x1)
        assertEquals(-50f, cmd.y1)
        assertEquals(200f, cmd.x)
        assertEquals(0f, cmd.y)
    }

    @Test
    fun testClose() {
        val result = SvgPathParser.parse("M 0 0 L 100 0 L 100 100 Z")
        assertEquals(4, result.commands.size)
        assertTrue(result.commands[3] is PathCommand.Close)
    }

    @Test
    fun testCloseReturnsToStart() {
        val result = SvgPathParser.parse("M 10 20 L 50 20 L 50 50 Z L 100 100")
        assertEquals(5, result.commands.size)
        // After Z, current position should be back at (10, 20)
        // So the absolute L 100 100 should work correctly
        val lastLine = result.commands[4] as PathCommand.LineTo
        assertEquals(100f, lastLine.x)
        assertEquals(100f, lastLine.y)
    }

    @Test
    fun testMultipleCoordinatesImplicitLineTo() {
        // After first M coordinates, subsequent coordinate pairs are implicit L
        val result = SvgPathParser.parse("M 0 0 10 10 20 20")
        assertEquals(3, result.commands.size)
        assertTrue(result.commands[0] is PathCommand.MoveTo)
        assertTrue(result.commands[1] is PathCommand.LineTo)
        assertTrue(result.commands[2] is PathCommand.LineTo)
    }

    @Test
    fun testScaleParameter() {
        val result = SvgPathParser.parse("M 10 20", scale = 2f)
        val cmd = result.commands[0] as PathCommand.MoveTo
        assertEquals(20f, cmd.x)
        assertEquals(40f, cmd.y)
    }

    @Test
    fun testOffsetParameters() {
        val result = SvgPathParser.parse("M 10 20", offsetX = 5f, offsetY = 10f)
        val cmd = result.commands[0] as PathCommand.MoveTo
        assertEquals(15f, cmd.x)
        assertEquals(30f, cmd.y)
    }

    @Test
    fun testArcCommand() {
        val result = SvgPathParser.parse("M 10 10 A 10 10 0 0 1 30 10")
        // Arc should be converted to one or more cubic Bezier curves
        assertTrue(result.commands.size >= 2)
        assertTrue(result.commands[0] is PathCommand.MoveTo)
        // Subsequent commands should be CubicTo (arc approximation)
        for (i in 1 until result.commands.size) {
            assertTrue(result.commands[i] is PathCommand.CubicTo)
        }
    }

    @Test
    fun testDegenerateArcBecomesLine() {
        // Arc with zero radius should become a line
        val result = SvgPathParser.parse("M 10 10 A 0 0 0 0 1 30 10")
        assertEquals(2, result.commands.size)
        assertTrue(result.commands[1] is PathCommand.LineTo)
    }

    @Test
    fun testCommasAndSpacesSeparators() {
        val result = SvgPathParser.parse("M10,20 L30,40")
        assertEquals(2, result.commands.size)
        val moveTo = result.commands[0] as PathCommand.MoveTo
        assertEquals(10f, moveTo.x)
        assertEquals(20f, moveTo.y)
        val lineTo = result.commands[1] as PathCommand.LineTo
        assertEquals(30f, lineTo.x)
        assertEquals(40f, lineTo.y)
    }

    @Test
    fun testNegativeNumbers() {
        val result = SvgPathParser.parse("M-10-20L-30-40")
        assertEquals(2, result.commands.size)
        val moveTo = result.commands[0] as PathCommand.MoveTo
        assertEquals(-10f, moveTo.x)
        assertEquals(-20f, moveTo.y)
    }

    @Test
    fun testDecimalNumbers() {
        val result = SvgPathParser.parse("M 10.5 20.25")
        val cmd = result.commands[0] as PathCommand.MoveTo
        assertEquals(10.5f, cmd.x)
        assertEquals(20.25f, cmd.y)
    }

    @Test
    fun testScientificNotation() {
        val result = SvgPathParser.parse("M 1e2 2e-1")
        val cmd = result.commands[0] as PathCommand.MoveTo
        assertEquals(100f, cmd.x)
        assertEquals(0.2f, cmd.y, 0.001f)
    }

    @Test
    fun testEmptyPath() {
        val result = SvgPathParser.parse("")
        assertEquals(0, result.commands.size)
    }

    @Test
    fun testComplexPath() {
        // A realistic SVG path
        val result = SvgPathParser.parse(
            "M256,48C141.31,48,48,141.31,48,256s93.31,208,208,208,208-93.31,208-208S370.69,48,256,48Z"
        )
        assertTrue(result.commands.isNotEmpty())
        assertTrue(result.commands.first() is PathCommand.MoveTo)
        assertTrue(result.commands.last() is PathCommand.Close)
    }
}
