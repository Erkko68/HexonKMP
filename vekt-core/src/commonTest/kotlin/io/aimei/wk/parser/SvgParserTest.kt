package io.aimei.wk.parser

import io.aimei.wk.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SvgParserTest {

    @Test
    fun testParseBasicSvg() {
        val svg = """
            <svg width="100" height="200" viewBox="0 0 100 200">
                <path d="M0 0 L100 100" fill="red"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(100f, doc.width)
        assertEquals(200f, doc.height)
        assertNotNull(doc.viewBox)
        assertEquals(0f, doc.viewBox!!.minX)
        assertEquals(0f, doc.viewBox!!.minY)
        assertEquals(100f, doc.viewBox!!.width)
        assertEquals(200f, doc.viewBox!!.height)
    }

    @Test
    fun testParseViewBox() {
        val svg = """<svg viewBox="10 20 100 200"></svg>"""
        val doc = SvgParser.parse(svg)
        assertNotNull(doc.viewBox)
        assertEquals(10f, doc.viewBox!!.minX)
        assertEquals(20f, doc.viewBox!!.minY)
        assertEquals(100f, doc.viewBox!!.width)
        assertEquals(200f, doc.viewBox!!.height)
    }

    @Test
    fun testParseViewBoxWithCommas() {
        val svg = """<svg viewBox="10,20,100,200"></svg>"""
        val doc = SvgParser.parse(svg)
        assertNotNull(doc.viewBox)
        assertEquals(10f, doc.viewBox!!.minX)
        assertEquals(20f, doc.viewBox!!.minY)
    }

    @Test
    fun testViewBoxWidth() {
        val svg = """<svg width="200" height="200" viewBox="0 0 100 100"></svg>"""
        val doc = SvgParser.parse(svg)
        assertEquals(100f, doc.viewBoxWidth)
        assertEquals(100f, doc.viewBoxHeight)
    }

    @Test
    fun testViewBoxWidthFallback() {
        val svg = """<svg width="200" height="150"></svg>"""
        val doc = SvgParser.parse(svg)
        assertEquals(200f, doc.viewBoxWidth)
        assertEquals(150f, doc.viewBoxHeight)
    }

    @Test
    fun testParsePath() {
        val svg = """
            <svg>
                <path d="M10 20 L30 40" fill="#ff0000" stroke="#00ff00" stroke-width="2"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.elements.size)
        val path = doc.elements[0] as PathElement
        assertEquals("M10 20 L30 40", path.pathData)
        assertTrue(path.style.fill is SvgColor.Rgb)
        assertEquals(0xFFFF0000, (path.style.fill as SvgColor.Rgb).argb)
        assertTrue(path.style.stroke is SvgColor.Rgb)
        assertEquals(2f, path.style.strokeWidth)
    }

    @Test
    fun testParsePathWithNone() {
        val svg = """
            <svg>
                <path d="M0 0" fill="none" stroke="black"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertTrue(path.style.fill is SvgColor.None)
        assertTrue(path.style.stroke is SvgColor.Rgb)
    }

    @Test
    fun testParseCircle() {
        val svg = """
            <svg>
                <circle cx="50" cy="60" r="25" fill="blue"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.elements.size)
        val circle = doc.elements[0] as CircleElement
        assertEquals(50f, circle.cx)
        assertEquals(60f, circle.cy)
        assertEquals(25f, circle.r)
    }

    @Test
    fun testParseEllipse() {
        val svg = """
            <svg>
                <ellipse cx="100" cy="100" rx="50" ry="25"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val ellipse = doc.elements[0] as EllipseElement
        assertEquals(100f, ellipse.cx)
        assertEquals(100f, ellipse.cy)
        assertEquals(50f, ellipse.rx)
        assertEquals(25f, ellipse.ry)
    }

    @Test
    fun testParseRect() {
        val svg = """
            <svg>
                <rect x="10" y="20" width="100" height="50" rx="5" ry="5"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val rect = doc.elements[0] as RectElement
        assertEquals(10f, rect.x)
        assertEquals(20f, rect.y)
        assertEquals(100f, rect.width)
        assertEquals(50f, rect.height)
        assertEquals(5f, rect.rx)
        assertEquals(5f, rect.ry)
    }

    @Test
    fun testParseLine() {
        val svg = """
            <svg>
                <line x1="10" y1="20" x2="100" y2="200" stroke="black"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val line = doc.elements[0] as LineElement
        assertEquals(10f, line.x1)
        assertEquals(20f, line.y1)
        assertEquals(100f, line.x2)
        assertEquals(200f, line.y2)
    }

    @Test
    fun testParsePolygon() {
        val svg = """
            <svg>
                <polygon points="100,10 40,198 190,78 10,78 160,198"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val polygon = doc.elements[0] as PolygonElement
        assertEquals(5, polygon.points.size)
        assertEquals(100f to 10f, polygon.points[0])
        assertEquals(40f to 198f, polygon.points[1])
    }

    @Test
    fun testParsePolyline() {
        val svg = """
            <svg>
                <polyline points="0,40 40,40 40,80 80,80" fill="none" stroke="black"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val polyline = doc.elements[0] as PolylineElement
        assertEquals(4, polyline.points.size)
    }

    @Test
    fun testParseGroup() {
        val svg = """
            <svg>
                <g fill="red">
                    <path d="M0 0"/>
                    <circle cx="50" cy="50" r="25"/>
                </g>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(1, doc.elements.size)
        val group = doc.elements[0] as GroupElement
        assertEquals(2, group.children.size)
        assertTrue(group.children[0] is PathElement)
        assertTrue(group.children[1] is CircleElement)
    }

    @Test
    fun testParseTransform() {
        val svg = """
            <svg>
                <path d="M0 0" transform="translate(10, 20)"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertNotNull(path.transform)
        assertTrue(path.transform is Transform.Translate)
        val translate = path.transform as Transform.Translate
        assertEquals(10f, translate.x)
        assertEquals(20f, translate.y)
    }

    @Test
    fun testParseNamedColor() {
        val svg = """
            <svg>
                <path d="M0 0" fill="coral"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertTrue(path.style.fill is SvgColor.Rgb)
        assertEquals(0xFFFF7F50, (path.style.fill as SvgColor.Rgb).argb)
    }

    @Test
    fun testParseHexColorShort() {
        val svg = """
            <svg>
                <path d="M0 0" fill="#f00"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertTrue(path.style.fill is SvgColor.Rgb)
        assertEquals(0xFFFF0000, (path.style.fill as SvgColor.Rgb).argb)
    }

    @Test
    fun testParseStrokeLineCap() {
        val svg = """
            <svg>
                <path d="M0 0" stroke-linecap="round"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertEquals(StrokeLineCap.Round, path.style.strokeLineCap)
    }

    @Test
    fun testParseStrokeLineJoin() {
        val svg = """
            <svg>
                <path d="M0 0" stroke-linejoin="bevel"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertEquals(StrokeLineJoin.Bevel, path.style.strokeLineJoin)
    }

    @Test
    fun testParseOpacity() {
        val svg = """
            <svg>
                <path d="M0 0" opacity="0.5" fill-opacity="0.7" stroke-opacity="0.3"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertEquals(0.5f, path.style.opacity)
        assertEquals(0.7f, path.style.fillOpacity)
        assertEquals(0.3f, path.style.strokeOpacity)
    }

    @Test
    fun testParseCssInlineStyle() {
        val svg = """
            <svg>
                <path d="M0 0" style="fill:#ff0000;stroke:#00ff00;stroke-width:2px"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val path = doc.elements[0] as PathElement
        assertTrue(path.style.fill is SvgColor.Rgb)
        assertTrue(path.style.stroke is SvgColor.Rgb)
        assertEquals(2f, path.style.strokeWidth)
    }

    @Test
    fun testParseComplexSvg() {
        // A real SVG with multiple paths and transforms
        val svg = """
            <svg width="512" height="512">
                <path d="M0 0 C2.81878924 0.6052425 5.05333422 1.45230596 7.546875 2.890625 Z" fill="#FEE2E0" transform="translate(127,8)"/>
                <path d="M0 0 C1.45703125 1.49023438 Z" fill="#B3E97F" transform="translate(201,360)"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        assertEquals(512f, doc.width)
        assertEquals(512f, doc.height)
        assertEquals(2, doc.elements.size)

        val path1 = doc.elements[0] as PathElement
        assertNotNull(path1.transform)

        val path2 = doc.elements[1] as PathElement
        assertNotNull(path2.transform)
    }
}
