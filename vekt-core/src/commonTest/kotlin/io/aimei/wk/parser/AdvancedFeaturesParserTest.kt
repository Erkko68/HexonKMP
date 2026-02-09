package io.aimei.wk.parser

import io.aimei.wk.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvancedFeaturesParserTest {

    @Test
    fun testParseClipPath() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <clipPath id="myClip">
                        <circle cx="100" cy="100" r="50"/>
                    </clipPath>
                </defs>
                <rect x="0" y="0" width="200" height="200" clip-path="url(#myClip)" fill="red"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.defs.clipPaths.size)
        assertNotNull(doc.defs.clipPaths["myClip"])

        val clipPath = doc.defs.clipPaths["myClip"]!!
        assertEquals(1, clipPath.elements.size)
        assertTrue(clipPath.elements[0] is CircleElement)

        val rect = doc.elements[0] as RectElement
        assertEquals("myClip", rect.style.clipPathId)
    }

    @Test
    fun testParseMask() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <mask id="myMask">
                        <rect x="0" y="0" width="100" height="100" fill="white"/>
                    </mask>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.defs.masks.size)
        assertNotNull(doc.defs.masks["myMask"])

        val mask = doc.defs.masks["myMask"]!!
        assertEquals(1, mask.elements.size)
        assertTrue(mask.elements[0] is RectElement)
    }

    @Test
    fun testParseFilter() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <filter id="blur">
                        <feGaussianBlur in="SourceGraphic" stdDeviation="5"/>
                    </filter>
                </defs>
                <rect filter="url(#blur)" x="10" y="10" width="100" height="100"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.defs.filters.size)
        assertNotNull(doc.defs.filters["blur"])

        val filter = doc.defs.filters["blur"]!!
        assertEquals(1, filter.primitives.size)
        assertTrue(filter.primitives[0] is FeGaussianBlur)
        assertEquals(5f, (filter.primitives[0] as FeGaussianBlur).stdDeviationX)

        val rect = doc.elements[0] as RectElement
        assertEquals("blur", rect.style.filterId)
    }

    @Test
    fun testParseFilterWithMultiplePrimitives() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <filter id="shadow">
                        <feOffset in="SourceAlpha" dx="2" dy="2" result="offsetBlur"/>
                        <feGaussianBlur in="offsetBlur" stdDeviation="3" result="blurred"/>
                        <feBlend in="SourceGraphic" in2="blurred" mode="multiply"/>
                    </filter>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val filter = doc.defs.filters["shadow"]!!
        assertEquals(3, filter.primitives.size)

        // Find primitives by type (order depends on parser implementation)
        val feOffset = filter.primitives.filterIsInstance<FeOffset>().first()
        assertEquals(2f, feOffset.dx)
        assertEquals(2f, feOffset.dy)
        assertEquals("offsetBlur", feOffset.result)

        val feBlur = filter.primitives.filterIsInstance<FeGaussianBlur>().first()
        assertEquals("offsetBlur", feBlur.input)
        assertEquals(3f, feBlur.stdDeviationX)

        val feBlend = filter.primitives.filterIsInstance<FeBlend>().first()
        assertEquals(BlendMode.Multiply, feBlend.mode)
    }

    @Test
    fun testParsePattern() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <pattern id="dots" width="10" height="10" patternUnits="userSpaceOnUse">
                        <circle cx="5" cy="5" r="3" fill="black"/>
                    </pattern>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.defs.patterns.size)
        assertNotNull(doc.defs.patterns["dots"])

        val pattern = doc.defs.patterns["dots"]!!
        assertEquals(10f, pattern.width)
        assertEquals(10f, pattern.height)
        assertEquals(PatternUnits.UserSpaceOnUse, pattern.patternUnits)
        assertEquals(1, pattern.elements.size)
        assertTrue(pattern.elements[0] is CircleElement)
    }

    @Test
    fun testParseImage() {
        val svg = """
            <svg width="200" height="200">
                <image x="10" y="20" width="100" height="80" href="data:image/png;base64,abc123" preserveAspectRatio="xMidYMin meet"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.elements.size)
        assertTrue(doc.elements[0] is ImageElement)

        val image = doc.elements[0] as ImageElement
        assertEquals(10f, image.x)
        assertEquals(20f, image.y)
        assertEquals(100f, image.width)
        assertEquals(80f, image.height)
        assertEquals("data:image/png;base64,abc123", image.href)
        assertEquals("xMidYMin meet", image.preserveAspectRatio)
    }

    @Test
    fun testParseImageWithXlinkHref() {
        val svg = """
            <svg width="200" height="200">
                <image x="0" y="0" width="50" height="50" xlink:href="image.png"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val image = doc.elements[0] as ImageElement
        assertEquals("image.png", image.href)
    }

    @Test
    fun testParseTSpan() {
        val svg = """
            <svg width="200" height="100">
                <text x="10" y="50">
                    <tspan fill="red">Hello</tspan>
                    <tspan dx="5" fill="blue">World</tspan>
                </text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.elements.size)
        assertTrue(doc.elements[0] is TextElement)

        val text = doc.elements[0] as TextElement
        assertEquals(2, text.spans.size)

        val span1 = text.spans[0]
        assertEquals("Hello", span1.text)
        assertTrue(span1.style?.fill is SvgColor.Rgb)
        assertEquals(0xFFFF0000L, (span1.style?.fill as SvgColor.Rgb).argb)

        val span2 = text.spans[1]
        assertEquals("World", span2.text)
        assertEquals(5f, span2.dx)
        assertTrue(span2.style?.fill is SvgColor.Rgb)
        assertEquals(0xFF0000FFL, (span2.style?.fill as SvgColor.Rgb).argb)
    }

    @Test
    fun testParseTSpanWithPositioning() {
        val svg = """
            <svg width="200" height="100">
                <text x="0" y="20">
                    <tspan x="10" y="30" font-size="20">Positioned</tspan>
                </text>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val text = doc.elements[0] as TextElement
        assertEquals(1, text.spans.size)

        val span = text.spans[0]
        assertEquals(10f, span.x)
        assertEquals(30f, span.y)
        assertEquals(20f, span.fontSize)
        assertEquals("Positioned", span.text)
    }

    @Test
    fun testParseColorMatrix() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <filter id="grayscale">
                        <feColorMatrix type="saturate" values="0"/>
                    </filter>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val filter = doc.defs.filters["grayscale"]!!
        assertEquals(1, filter.primitives.size)

        val colorMatrix = filter.primitives[0] as FeColorMatrix
        assertEquals(ColorMatrixType.Saturate, colorMatrix.type)
        assertEquals(listOf(0f), colorMatrix.values)
    }

    @Test
    fun testParseDropShadow() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <filter id="shadow">
                        <feDropShadow dx="3" dy="3" stdDeviation="2" flood-color="black" flood-opacity="0.5"/>
                    </filter>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val filter = doc.defs.filters["shadow"]!!
        assertEquals(1, filter.primitives.size)

        val dropShadow = filter.primitives[0] as FeDropShadow
        assertEquals(3f, dropShadow.dx)
        assertEquals(3f, dropShadow.dy)
        assertEquals(2f, dropShadow.stdDeviation)
        assertEquals(0.5f, dropShadow.floodOpacity)
    }

    @Test
    fun testParseMaskAttribute() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <mask id="myMask">
                        <rect width="100" height="100" fill="white"/>
                    </mask>
                </defs>
                <rect mask="url(#myMask)" width="200" height="200" fill="red"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val rect = doc.elements[0] as RectElement
        assertEquals("myMask", rect.style.maskId)
    }

    @Test
    fun testParseClipPathUnits() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <clipPath id="myClip" clipPathUnits="objectBoundingBox">
                        <rect x="0" y="0" width="0.5" height="0.5"/>
                    </clipPath>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val clipPath = doc.defs.clipPaths["myClip"]!!
        assertEquals(ClipPathUnits.ObjectBoundingBox, clipPath.clipPathUnits)
    }

    @Test
    fun testParseMaskUnits() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <mask id="myMask" maskUnits="userSpaceOnUse" maskContentUnits="objectBoundingBox">
                        <rect x="0" y="0" width="100" height="100" fill="white"/>
                    </mask>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val mask = doc.defs.masks["myMask"]!!
        assertEquals(MaskUnits.UserSpaceOnUse, mask.maskUnits)
        assertEquals(MaskUnits.ObjectBoundingBox, mask.maskContentUnits)
    }
}
