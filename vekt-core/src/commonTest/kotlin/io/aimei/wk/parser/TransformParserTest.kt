package io.aimei.wk.parser

import io.aimei.wk.model.Transform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransformParserTest {

    @Test
    fun testParseTranslate() {
        val result = TransformParser.parse("translate(10, 20)")
        assertNotNull(result)
        assertTrue(result is Transform.Translate)
        assertEquals(10f, (result as Transform.Translate).x)
        assertEquals(20f, result.y)
    }

    @Test
    fun testParseTranslateSpaceSeparated() {
        val result = TransformParser.parse("translate(10 20)")
        assertNotNull(result)
        assertTrue(result is Transform.Translate)
        assertEquals(10f, (result as Transform.Translate).x)
        assertEquals(20f, result.y)
    }

    @Test
    fun testParseTranslateSingleValue() {
        val result = TransformParser.parse("translate(15)")
        assertNotNull(result)
        assertTrue(result is Transform.Translate)
        assertEquals(15f, (result as Transform.Translate).x)
        assertEquals(0f, result.y)
    }

    @Test
    fun testParseScale() {
        val result = TransformParser.parse("scale(2, 3)")
        assertNotNull(result)
        assertTrue(result is Transform.Scale)
        assertEquals(2f, (result as Transform.Scale).x)
        assertEquals(3f, result.y)
    }

    @Test
    fun testParseScaleUniform() {
        val result = TransformParser.parse("scale(2)")
        assertNotNull(result)
        assertTrue(result is Transform.Scale)
        assertEquals(2f, (result as Transform.Scale).x)
        assertEquals(2f, result.y)
    }

    @Test
    fun testParseRotate() {
        val result = TransformParser.parse("rotate(45)")
        assertNotNull(result)
        assertTrue(result is Transform.Rotate)
        assertEquals(45f, (result as Transform.Rotate).angle)
        assertEquals(0f, result.cx)
        assertEquals(0f, result.cy)
    }

    @Test
    fun testParseRotateWithCenter() {
        val result = TransformParser.parse("rotate(90, 100, 150)")
        assertNotNull(result)
        assertTrue(result is Transform.Rotate)
        assertEquals(90f, (result as Transform.Rotate).angle)
        assertEquals(100f, result.cx)
        assertEquals(150f, result.cy)
    }

    @Test
    fun testParseSkewX() {
        val result = TransformParser.parse("skewX(30)")
        assertNotNull(result)
        assertTrue(result is Transform.SkewX)
        assertEquals(30f, (result as Transform.SkewX).angle)
    }

    @Test
    fun testParseSkewY() {
        val result = TransformParser.parse("skewY(45)")
        assertNotNull(result)
        assertTrue(result is Transform.SkewY)
        assertEquals(45f, (result as Transform.SkewY).angle)
    }

    @Test
    fun testParseMatrix() {
        val result = TransformParser.parse("matrix(1, 0, 0, 1, 10, 20)")
        assertNotNull(result)
        assertTrue(result is Transform.Matrix)
        assertEquals(1f, (result as Transform.Matrix).a)
        assertEquals(0f, result.b)
        assertEquals(0f, result.c)
        assertEquals(1f, result.d)
        assertEquals(10f, result.e)
        assertEquals(20f, result.f)
    }

    @Test
    fun testParseCombinedTransforms() {
        val result = TransformParser.parse("translate(10, 20) rotate(45) scale(2)")
        assertNotNull(result)
        assertTrue(result is Transform.Combined)
        val combined = result as Transform.Combined
        assertEquals(3, combined.transforms.size)
        assertTrue(combined.transforms[0] is Transform.Translate)
        assertTrue(combined.transforms[1] is Transform.Rotate)
        assertTrue(combined.transforms[2] is Transform.Scale)
    }

    @Test
    fun testParseCombinedOrder() {
        val result = TransformParser.parse("scale(2) translate(10, 20)")
        assertNotNull(result)
        assertTrue(result is Transform.Combined)
        val combined = result as Transform.Combined
        assertEquals(2, combined.transforms.size)
        assertTrue(combined.transforms[0] is Transform.Scale)
        assertTrue(combined.transforms[1] is Transform.Translate)
    }

    @Test
    fun testParseEmpty() {
        val result = TransformParser.parse("")
        assertNull(result)
    }

    @Test
    fun testParseInvalid() {
        val result = TransformParser.parse("invalid()")
        assertNull(result)
    }

    @Test
    fun testCaseInsensitive() {
        val result = TransformParser.parse("TRANSLATE(10, 20)")
        assertNotNull(result)
        assertTrue(result is Transform.Translate)
    }

    @Test
    fun testDegreesToRadians() {
        val radians = TransformParser.degreesToRadians(180f)
        assertEquals(kotlin.math.PI.toFloat(), radians, 0.001f)
    }

    @Test
    fun testNoSpaceBetweenTransforms() {
        val result = TransformParser.parse("translate(10,20)rotate(45)")
        assertNotNull(result)
        assertTrue(result is Transform.Combined)
        assertEquals(2, (result as Transform.Combined).transforms.size)
    }
}
