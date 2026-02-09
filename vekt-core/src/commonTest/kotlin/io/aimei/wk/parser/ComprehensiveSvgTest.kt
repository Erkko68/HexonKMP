package io.aimei.wk.parser

import io.aimei.wk.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests parsing of comprehensive SVG which covers all major SVG features.
 */
class ComprehensiveSvgTest {

    private val comprehensiveSvg = """
        <?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg"
             xmlns:xlink="http://www.w3.org/1999/xlink"
             viewBox="0 0 800 1200"
             width="800" height="1200"
             preserveAspectRatio="xMidYMid meet">
          <title>Comprehensive SVG Test</title>
          <desc>A comprehensive test SVG covering all major features</desc>

          <!-- Definitions -->
          <defs>
            <linearGradient id="linearGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" style="stop-color:#FF0000;stop-opacity:1" />
              <stop offset="50%" style="stop-color:#00FF00;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#0000FF;stop-opacity:1" />
            </linearGradient>

            <radialGradient id="radialGrad1" cx="50%" cy="50%" r="50%" fx="25%" fy="25%">
              <stop offset="0%" stop-color="white" />
              <stop offset="50%" stop-color="orange" />
              <stop offset="100%" stop-color="red" />
            </radialGradient>

            <clipPath id="circleClip">
              <circle cx="50" cy="50" r="40" />
            </clipPath>

            <symbol id="star" viewBox="0 0 100 100">
              <polygon points="50,5 61,40 98,40 68,62 79,97 50,75 21,97 32,62 2,40 39,40" fill="gold"/>
            </symbol>
          </defs>

          <!-- Basic Shapes -->
          <g id="basicShapes">
            <rect x="10" y="10" width="100" height="50" rx="5" ry="5" fill="#3498db"/>
            <circle cx="200" cy="35" r="25" fill="url(#radialGrad1)"/>
            <ellipse cx="300" cy="35" rx="40" ry="20" fill="url(#linearGrad1)"/>
            <line x1="350" y1="10" x2="400" y2="60" stroke="black" stroke-width="2"/>
            <polyline points="410,60 430,10 450,40 470,20 490,50" fill="none" stroke="purple" stroke-width="2"/>
            <polygon points="530,10 560,60 500,60" fill="orange"/>
          </g>

          <!-- Paths with various commands -->
          <g id="paths" transform="translate(0,100)">
            <!-- Cubic Bezier -->
            <path d="M10,50 C30,10 70,10 90,50 S150,90 170,50" fill="none" stroke="red" stroke-width="2"/>
            <!-- Quadratic Bezier -->
            <path d="M200,50 Q250,10 300,50 T400,50" fill="none" stroke="green" stroke-width="2"/>
            <!-- Arc -->
            <path d="M450,50 A30,20 0 1,1 510,50 A30,20 0 1,0 570,50" fill="none" stroke="blue" stroke-width="2"/>
            <!-- Relative commands -->
            <path d="M600,50 l50,-30 l50,30 l-25,20 z" fill="cyan" stroke="navy" stroke-width="1"/>
          </g>

          <!-- Transforms -->
          <g id="transforms" transform="translate(0,200)">
            <rect x="10" y="10" width="40" height="40" fill="red" transform="rotate(45 30 30)"/>
            <rect x="100" y="10" width="40" height="40" fill="green" transform="scale(1.5)"/>
            <rect x="250" y="10" width="40" height="40" fill="blue" transform="skewX(20)"/>
            <g transform="translate(350,0) rotate(30) scale(0.8)">
              <rect x="10" y="10" width="40" height="40" fill="purple"/>
            </g>
          </g>

          <!-- Stroke properties -->
          <g id="strokes" transform="translate(0,300)">
            <line x1="10" y1="30" x2="100" y2="30" stroke="black" stroke-width="10" stroke-linecap="butt"/>
            <line x1="120" y1="30" x2="210" y2="30" stroke="black" stroke-width="10" stroke-linecap="round"/>
            <line x1="230" y1="30" x2="320" y2="30" stroke="black" stroke-width="10" stroke-linecap="square"/>
            <polyline points="340,10 360,50 380,10 400,50" fill="none" stroke="red" stroke-width="5" stroke-linejoin="miter"/>
            <polyline points="420,10 440,50 460,10 480,50" fill="none" stroke="green" stroke-width="5" stroke-linejoin="round"/>
            <polyline points="500,10 520,50 540,10 560,50" fill="none" stroke="blue" stroke-width="5" stroke-linejoin="bevel"/>
          </g>

          <!-- Opacity -->
          <g id="opacity" transform="translate(0,370)">
            <rect x="10" y="10" width="60" height="40" fill="red" opacity="0.3"/>
            <rect x="50" y="10" width="60" height="40" fill="blue" fill-opacity="0.5"/>
            <rect x="90" y="10" width="60" height="40" fill="green" stroke="black" stroke-width="3" stroke-opacity="0.7"/>
          </g>

          <!-- Color formats -->
          <g id="colors" transform="translate(0,440)">
            <rect x="10" y="10" width="40" height="30" fill="#FF5733"/>
            <rect x="60" y="10" width="40" height="30" fill="#F00"/>
            <rect x="110" y="10" width="40" height="30" fill="rgb(100,150,200)"/>
            <rect x="160" y="10" width="40" height="30" fill="rgba(100,150,200,0.5)"/>
            <rect x="210" y="10" width="40" height="30" fill="coral"/>
          </g>

          <!-- Use and Symbol -->
          <g id="useSymbol" transform="translate(0,500)">
            <use xlink:href="#star" x="10" y="10" width="50" height="50"/>
            <use href="#star" x="70" y="10" width="50" height="50" transform="rotate(30 95 35)"/>
          </g>

          <!-- Clip Path -->
          <g id="clipPathTest" transform="translate(200,500)">
            <g clip-path="url(#circleClip)">
              <rect x="0" y="0" width="100" height="100" fill="url(#linearGrad1)"/>
            </g>
          </g>

          <!-- Text (basic) -->
          <g id="text" transform="translate(0,620)">
            <text x="10" y="30" font-size="24" fill="black">Hello SVG</text>
            <text x="200" y="30" font-size="20" fill="red" font-weight="bold">Bold Text</text>
            <text x="350" y="30" font-size="18" fill="blue" font-style="italic">Italic</text>
          </g>

          <!-- Nested groups -->
          <g id="nested" transform="translate(0,670)">
            <g fill="red" opacity="0.8">
              <g transform="translate(10,0)">
                <rect x="0" y="10" width="30" height="30"/>
                <g transform="translate(40,0)">
                  <rect x="0" y="10" width="30" height="30" fill="blue"/>
                </g>
              </g>
            </g>
          </g>

          <!-- Edge cases -->
          <g id="edgeCases" transform="translate(0,730)">
            <!-- Zero dimensions -->
            <rect x="10" y="10" width="0" height="30" fill="red"/>
            <circle cx="50" cy="25" r="0" fill="blue"/>
            <!-- Negative coordinates -->
            <rect x="-10" y="10" width="30" height="20" fill="green" transform="translate(100,0)"/>
            <!-- Scientific notation -->
            <circle cx="1.5e2" cy="25" r="1e1" fill="purple"/>
            <!-- Multiple decimals -->
            <path d="M200.5.5 L230.5.5 L230.5 40.5 Z" fill="orange"/>
          </g>

        </svg>
    """.trimIndent()

    @Test
    fun testParseComprehensiveSvg() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Basic document properties
        assertEquals(800f, doc.width)
        assertEquals(1200f, doc.height)
        assertNotNull(doc.viewBox)
        assertEquals(0f, doc.viewBox!!.minX)
        assertEquals(0f, doc.viewBox!!.minY)
        assertEquals(800f, doc.viewBox!!.width)
        assertEquals(1200f, doc.viewBox!!.height)
    }

    @Test
    fun testParseDefs() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Should have gradients defined
        assertTrue(doc.defs.gradients.isNotEmpty())

        // Check linear gradient
        val linearGrad = doc.defs.gradients["linearGrad1"]
        assertNotNull(linearGrad)
        assertTrue(linearGrad is LinearGradient)
        assertEquals(3, (linearGrad as LinearGradient).stops.size)

        // Check radial gradient
        val radialGrad = doc.defs.gradients["radialGrad1"]
        assertNotNull(radialGrad)
        assertTrue(radialGrad is RadialGradient)
    }

    @Test
    fun testParseBasicShapesGroup() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Find first group (basicShapes)
        val groups = doc.elements.filterIsInstance<GroupElement>()
        assertTrue(groups.isNotEmpty())
        val basicShapesGroup = groups.first()

        // Should contain rect, circle, ellipse, line, polyline, polygon
        val children = basicShapesGroup.children
        assertTrue(children.any { it is RectElement })
        assertTrue(children.any { it is CircleElement })
        assertTrue(children.any { it is EllipseElement })
        assertTrue(children.any { it is LineElement })
        assertTrue(children.any { it is PolylineElement })
        assertTrue(children.any { it is PolygonElement })
    }

    @Test
    fun testParsePathsGroup() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Find groups with transforms
        val groups = doc.elements.filterIsInstance<GroupElement>()
        val pathsGroup = groups.find { it.transform != null && it.transform is Transform.Translate }
        assertNotNull(pathsGroup)

        // Should contain paths
        val paths = pathsGroup.children.filterIsInstance<PathElement>()
        assertTrue(paths.isNotEmpty())
    }

    @Test
    fun testParseTransformsGroup() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Get all groups
        val groups = doc.elements.filterIsInstance<GroupElement>()
        assertTrue(groups.size >= 3)

        // Find elements with various transforms
        var hasRotate = false
        var hasScale = false
        var hasSkew = false

        fun checkTransforms(elements: List<SvgElement>) {
            for (element in elements) {
                when (element.transform) {
                    is Transform.Rotate -> hasRotate = true
                    is Transform.Scale -> hasScale = true
                    is Transform.SkewX -> hasSkew = true
                    is Transform.Combined -> {
                        val combined = element.transform as Transform.Combined
                        for (t in combined.transforms) {
                            when (t) {
                                is Transform.Rotate -> hasRotate = true
                                is Transform.Scale -> hasScale = true
                                is Transform.SkewX -> hasSkew = true
                                else -> {}
                            }
                        }
                    }
                    else -> {}
                }
                if (element is GroupElement) {
                    checkTransforms(element.children)
                }
            }
        }

        checkTransforms(doc.elements)
        assertTrue(hasRotate)
        assertTrue(hasScale)
        assertTrue(hasSkew)
    }

    @Test
    fun testParseStrokeProperties() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all lines and polylines
        val allLines = mutableListOf<LineElement>()
        val allPolylines = mutableListOf<PolylineElement>()

        fun collectElements(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is LineElement -> allLines.add(element)
                    is PolylineElement -> allPolylines.add(element)
                    is GroupElement -> collectElements(element.children)
                    else -> {}
                }
            }
        }

        collectElements(doc.elements)

        // Check linecap variations
        assertTrue(allLines.any { it.style.strokeLineCap == StrokeLineCap.Butt })
        assertTrue(allLines.any { it.style.strokeLineCap == StrokeLineCap.Round })
        assertTrue(allLines.any { it.style.strokeLineCap == StrokeLineCap.Square })

        // Check linejoin variations
        assertTrue(allPolylines.any { it.style.strokeLineJoin == StrokeLineJoin.Miter })
        assertTrue(allPolylines.any { it.style.strokeLineJoin == StrokeLineJoin.Round })
        assertTrue(allPolylines.any { it.style.strokeLineJoin == StrokeLineJoin.Bevel })
    }

    @Test
    fun testParseOpacity() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all rects
        val allRects = mutableListOf<RectElement>()

        fun collectRects(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is RectElement -> allRects.add(element)
                    is GroupElement -> collectRects(element.children)
                    else -> {}
                }
            }
        }

        collectRects(doc.elements)

        // Check opacity values
        assertTrue(allRects.any { it.style.opacity < 1f })
        assertTrue(allRects.any { it.style.fillOpacity < 1f })
        assertTrue(allRects.any { it.style.strokeOpacity < 1f })
    }

    @Test
    fun testParseColorFormats() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all rects
        val allRects = mutableListOf<RectElement>()

        fun collectRects(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is RectElement -> allRects.add(element)
                    is GroupElement -> collectRects(element.children)
                    else -> {}
                }
            }
        }

        collectRects(doc.elements)

        // All should have parsed fill colors
        assertTrue(allRects.all { it.style.fill != null })
    }

    @Test
    fun testParseGradientReferences() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Find elements with gradient fills
        val allCircles = mutableListOf<CircleElement>()
        val allEllipses = mutableListOf<EllipseElement>()

        fun collectElements(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is CircleElement -> allCircles.add(element)
                    is EllipseElement -> allEllipses.add(element)
                    is GroupElement -> collectElements(element.children)
                    else -> {}
                }
            }
        }

        collectElements(doc.elements)

        // Some circles and ellipses should have gradient fills
        assertTrue(allCircles.any { it.style.fill is SvgColor.Reference })
        assertTrue(allEllipses.any { it.style.fill is SvgColor.Reference })
    }

    @Test
    fun testParseNestedGroups() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Count groups
        var groupCount = 0

        fun countGroups(elements: List<SvgElement>) {
            for (element in elements) {
                if (element is GroupElement) {
                    groupCount++
                    countGroups(element.children)
                }
            }
        }

        countGroups(doc.elements)
        assertTrue(groupCount >= 10) // Should have many groups
    }

    @Test
    fun testParseClipPath() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Check clip paths are defined
        assertTrue(doc.defs.clipPaths.isNotEmpty())
        assertNotNull(doc.defs.clipPaths["circleClip"])
    }

    @Test
    fun testParseEdgeCases() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all elements
        val allRects = mutableListOf<RectElement>()
        val allCircles = mutableListOf<CircleElement>()

        fun collectElements(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is RectElement -> allRects.add(element)
                    is CircleElement -> allCircles.add(element)
                    is GroupElement -> collectElements(element.children)
                    else -> {}
                }
            }
        }

        collectElements(doc.elements)

        // Zero dimension rect should parse
        assertTrue(allRects.any { it.width == 0f })

        // Zero radius circle should parse
        assertTrue(allCircles.any { it.r == 0f })

        // Scientific notation should parse - cx=150f (1.5e2), r=10f (1e1)
        assertTrue(allCircles.any { it.cx == 150f && it.r == 10f })

        // Negative coordinates with transform should parse
        assertTrue(allRects.any { it.x < 0f })
    }

    @Test
    fun testNoParseErrors() {
        // The main test - parsing should complete without exceptions
        val doc = SvgParser.parse(comprehensiveSvg)

        // Should have multiple top-level groups
        assertTrue(doc.elements.size >= 9)

        // Count total elements recursively
        fun countElements(elements: List<SvgElement>): Int {
            var count = elements.size
            for (element in elements) {
                if (element is GroupElement) {
                    count += countElements(element.children)
                }
            }
            return count
        }

        val totalElements = countElements(doc.elements)
        assertTrue(totalElements >= 40) // Should have many elements
    }

    @Test
    fun testParseText() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all text elements
        val allTexts = mutableListOf<TextElement>()

        fun collectTexts(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is TextElement -> allTexts.add(element)
                    is GroupElement -> collectTexts(element.children)
                    else -> {}
                }
            }
        }

        collectTexts(doc.elements)

        // Should have text elements
        assertTrue(allTexts.isNotEmpty())
        assertTrue(allTexts.any { it.text.contains("Hello") || it.text.contains("SVG") })
    }

    @Test
    fun testParseUseElement() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Collect all use elements
        val allUses = mutableListOf<UseElement>()

        fun collectUses(elements: List<SvgElement>) {
            for (element in elements) {
                when (element) {
                    is UseElement -> allUses.add(element)
                    is GroupElement -> collectUses(element.children)
                    else -> {}
                }
            }
        }

        collectUses(doc.elements)

        // Should have use elements referencing the star symbol
        assertTrue(allUses.isNotEmpty())
        assertTrue(allUses.any { it.href.contains("star") })
    }

    @Test
    fun testParseSymbol() {
        val doc = SvgParser.parse(comprehensiveSvg)

        // Check symbols are defined
        assertTrue(doc.defs.symbols.isNotEmpty())
        assertNotNull(doc.defs.symbols["star"])

        val star = doc.defs.symbols["star"]!!
        assertNotNull(star.viewBox)
        assertTrue(star.elements.isNotEmpty())
    }
}
