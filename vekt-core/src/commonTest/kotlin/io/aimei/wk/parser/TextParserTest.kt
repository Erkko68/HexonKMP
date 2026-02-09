package io.aimei.wk.parser

import io.aimei.wk.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextParserTest {

    @Test
    fun testParseTextElement() {
        val svg = """
            <svg width="200" height="100">
                <text x="10" y="50" fill="#FF0000">Hello World</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        val textElements = doc.elements.filterIsInstance<TextElement>()
        assertEquals(1, textElements.size)

        val text = textElements[0]
        assertEquals(10f, text.x)
        assertEquals(50f, text.y)
        assertEquals("Hello World", text.text)
        assertEquals(0xFFFF0000L, (text.style.fill as SvgColor.Rgb).argb)
    }

    @Test
    fun testParseTextWithFontAttributes() {
        val svg = """
            <svg width="200" height="100">
                <text x="0" y="0" font-family="Arial" font-size="24" font-weight="bold" font-style="italic">Styled Text</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements.filterIsInstance<TextElement>().first()

        assertEquals("Arial", text.fontFamily)
        assertEquals(24f, text.fontSize)
        assertEquals(FontWeight.Bold, text.fontWeight)
        assertEquals(FontStyle.Italic, text.fontStyle)
    }

    @Test
    fun testParseTextAnchor() {
        val svg = """
            <svg width="200" height="100">
                <text x="100" y="50" text-anchor="middle">Centered</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements.filterIsInstance<TextElement>().first()

        assertEquals(TextAnchor.Middle, text.textAnchor)
    }

    @Test
    fun testParseDominantBaseline() {
        val svg = """
            <svg width="200" height="100">
                <text x="10" y="50" dominant-baseline="hanging">Hanging</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements.filterIsInstance<TextElement>().first()

        assertEquals(DominantBaseline.Hanging, text.dominantBaseline)
    }

    @Test
    fun testParseTextWithStyleAttribute() {
        val svg = """
            <svg width="200" height="100">
                <text x="0" y="0" style="font-size:32px;font-weight:700;fill:#00FF00">CSS Styled</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements.filterIsInstance<TextElement>().first()

        assertEquals(32f, text.fontSize)
        assertEquals(FontWeight.W700, text.fontWeight)
        assertEquals(0xFF00FF00L, (text.style.fill as SvgColor.Rgb).argb)
    }

    @Test
    fun testParseTextWithTransform() {
        val svg = """
            <svg width="200" height="100">
                <text x="50" y="50" transform="rotate(45)">Rotated</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements.filterIsInstance<TextElement>().first()

        assertTrue(text.transform is Transform.Rotate)
        assertEquals(45f, (text.transform as Transform.Rotate).angle)
    }

    @Test
    fun testParseMultipleTextElements() {
        val svg = """
            <svg width="200" height="200">
                <text x="10" y="30">First</text>
                <text x="10" y="60">Second</text>
                <text x="10" y="90">Third</text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val textElements = doc.elements.filterIsInstance<TextElement>()

        assertEquals(3, textElements.size)
        assertEquals("First", textElements[0].text)
        assertEquals("Second", textElements[1].text)
        assertEquals("Third", textElements[2].text)
    }
}
