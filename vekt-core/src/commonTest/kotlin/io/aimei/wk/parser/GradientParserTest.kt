package io.aimei.wk.parser

import io.aimei.wk.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GradientParserTest {

    @Test
    fun testParseLinearGradient() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stop-color="#FF0000"/>
                        <stop offset="100%" stop-color="#0000FF"/>
                    </linearGradient>
                </defs>
                <rect x="0" y="0" width="100" height="100" fill="url(#grad1)"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        // Check gradient was parsed
        assertEquals(1, doc.defs.gradients.size)
        val gradient = doc.defs.gradients["grad1"]
        assertNotNull(gradient)
        assertTrue(gradient is LinearGradient)

        val linear = gradient as LinearGradient
        assertEquals(0f, linear.x1)
        assertEquals(0f, linear.y1)
        assertEquals(1f, linear.x2)
        assertEquals(0f, linear.y2)
        assertEquals(2, linear.stops.size)

        // Check stops
        assertEquals(0f, linear.stops[0].offset)
        assertEquals(0xFFFF0000L, linear.stops[0].color.argb)
        assertEquals(1f, linear.stops[1].offset)
        assertEquals(0xFF0000FFL, linear.stops[1].color.argb)

        // Check rect references gradient
        val rect = doc.elements.filterIsInstance<RectElement>().first()
        assertTrue(rect.style.fill is SvgColor.Reference)
        assertEquals("grad1", (rect.style.fill as SvgColor.Reference).id)
    }

    @Test
    fun testParseRadialGradient() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <radialGradient id="grad2" cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stop-color="#FFFFFF"/>
                        <stop offset="50%" stop-color="#FF0000"/>
                        <stop offset="100%" stop-color="#000000"/>
                    </radialGradient>
                </defs>
                <circle cx="50" cy="50" r="40" fill="url(#grad2)"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        val gradient = doc.defs.gradients["grad2"]
        assertNotNull(gradient)
        assertTrue(gradient is RadialGradient)

        val radial = gradient as RadialGradient
        assertEquals(0.5f, radial.cx)
        assertEquals(0.5f, radial.cy)
        assertEquals(0.5f, radial.r)
        assertEquals(3, radial.stops.size)
    }

    @Test
    fun testParseGradientWithAbsoluteCoords() {
        val svg = """
            <svg width="200" height="200">
                <defs>
                    <linearGradient id="grad3" x1="0" y1="0" x2="200" y2="200" gradientUnits="userSpaceOnUse">
                        <stop offset="0" stop-color="red"/>
                        <stop offset="1" stop-color="blue"/>
                    </linearGradient>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val gradient = doc.defs.gradients["grad3"] as? LinearGradient
        assertNotNull(gradient)
        assertEquals(GradientUnits.UserSpaceOnUse, gradient.gradientUnits)
        assertEquals(0f, gradient.x1)
        assertEquals(0f, gradient.y1)
        assertEquals(200f, gradient.x2)
        assertEquals(200f, gradient.y2)
    }

    @Test
    fun testParseGradientWithSpreadMethod() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <linearGradient id="padGrad" spreadMethod="pad">
                        <stop offset="0%" stop-color="red"/>
                        <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                    <linearGradient id="reflectGrad" spreadMethod="reflect">
                        <stop offset="0%" stop-color="red"/>
                        <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                    <linearGradient id="repeatGrad" spreadMethod="repeat">
                        <stop offset="0%" stop-color="red"/>
                        <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        assertEquals(SpreadMethod.Pad, (doc.defs.gradients["padGrad"] as LinearGradient).spreadMethod)
        assertEquals(SpreadMethod.Reflect, (doc.defs.gradients["reflectGrad"] as LinearGradient).spreadMethod)
        assertEquals(SpreadMethod.Repeat, (doc.defs.gradients["repeatGrad"] as LinearGradient).spreadMethod)
    }

    @Test
    fun testParseGradientStopOpacity() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <linearGradient id="grad">
                        <stop offset="0%" stop-color="#FF0000" stop-opacity="0.5"/>
                        <stop offset="100%" stop-color="#0000FF" stop-opacity="1"/>
                    </linearGradient>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val gradient = doc.defs.gradients["grad"] as LinearGradient
        assertEquals(0.5f, gradient.stops[0].opacity)
        assertEquals(1f, gradient.stops[1].opacity)
    }

    @Test
    fun testParseStopColorFromStyle() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <linearGradient id="grad">
                        <stop offset="0%" style="stop-color:#FF0000;stop-opacity:0.8"/>
                        <stop offset="100%" style="stop-color:#00FF00"/>
                    </linearGradient>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)
        val gradient = doc.defs.gradients["grad"] as LinearGradient
        assertEquals(0xFFFF0000L, gradient.stops[0].color.argb)
        assertEquals(0.8f, gradient.stops[0].opacity)
        assertEquals(0xFF00FF00L, gradient.stops[1].color.argb)
    }

    @Test
    fun testParseSymbol() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <symbol id="myIcon" viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="10" fill="red"/>
                        <path d="M12 6v12" stroke="white"/>
                    </symbol>
                </defs>
                <use href="#myIcon" x="10" y="10" width="50" height="50"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        // Check symbol was parsed
        assertEquals(1, doc.defs.symbols.size)
        val symbol = doc.defs.symbols["myIcon"]
        assertNotNull(symbol)
        assertNotNull(symbol.viewBox)
        assertEquals(24f, symbol.viewBox!!.width)
        assertEquals(2, symbol.elements.size)

        // Check use element was parsed
        val useElements = doc.elements.filterIsInstance<UseElement>()
        assertEquals(1, useElements.size)
        assertEquals("myIcon", useElements[0].href)
        assertEquals(10f, useElements[0].x)
        assertEquals(10f, useElements[0].y)
    }

    @Test
    fun testParseClipPath() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <clipPath id="myClip">
                        <circle cx="50" cy="50" r="40"/>
                    </clipPath>
                </defs>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        assertEquals(1, doc.defs.clipPaths.size)
        val clipPath = doc.defs.clipPaths["myClip"]
        assertNotNull(clipPath)
        assertEquals(1, clipPath.elements.size)
        assertTrue(clipPath.elements[0] is CircleElement)
    }

    @Test
    fun testParseUrlReference() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <linearGradient id="myGrad">
                        <stop offset="0%" stop-color="red"/>
                        <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                </defs>
                <rect fill="url(#myGrad)" x="0" y="0" width="100" height="100"/>
                <circle stroke="url(#myGrad)" fill="none" cx="50" cy="50" r="30"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        val rect = doc.elements.filterIsInstance<RectElement>().first()
        assertTrue(rect.style.fill is SvgColor.Reference)
        assertEquals("myGrad", (rect.style.fill as SvgColor.Reference).id)

        val circle = doc.elements.filterIsInstance<CircleElement>().first()
        assertTrue(circle.style.stroke is SvgColor.Reference)
        assertEquals("myGrad", (circle.style.stroke as SvgColor.Reference).id)
    }

    @Test
    fun testParseXlinkHref() {
        val svg = """
            <svg width="100" height="100">
                <defs>
                    <symbol id="icon">
                        <rect width="10" height="10"/>
                    </symbol>
                </defs>
                <use xlink:href="#icon" x="20" y="20"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        val useElements = doc.elements.filterIsInstance<UseElement>()
        assertEquals(1, useElements.size)
        assertEquals("icon", useElements[0].href)
    }

    @Test
    fun testGradientOutsideDefs() {
        // Some SVGs define gradients outside of defs
        val svg = """
            <svg width="100" height="100">
                <linearGradient id="grad1">
                    <stop offset="0%" stop-color="red"/>
                    <stop offset="100%" stop-color="blue"/>
                </linearGradient>
                <rect fill="url(#grad1)" x="0" y="0" width="100" height="100"/>
            </svg>
        """.trimIndent()

        val doc = SvgParser.parse(svg)

        assertEquals(1, doc.defs.gradients.size)
        assertNotNull(doc.defs.gradients["grad1"])
    }
}
