package io.aimei.wk.model

/**
 * Parsed SVG document data
 */
data class SvgDocument(
    val width: Float,
    val height: Float,
    val viewBox: ViewBox?,
    val elements: List<SvgElement>,
    val defs: SvgDefs = SvgDefs()
) {
    /**
     * Get the effective viewBox width (viewBox.width or width attribute)
     */
    val viewBoxWidth: Float get() = viewBox?.width ?: width

    /**
     * Get the effective viewBox height (viewBox.height or height attribute)
     */
    val viewBoxHeight: Float get() = viewBox?.height ?: height

    /**
     * Get the viewBox offset X
     */
    val viewBoxMinX: Float get() = viewBox?.minX ?: 0f

    /**
     * Get the viewBox offset Y
     */
    val viewBoxMinY: Float get() = viewBox?.minY ?: 0f
}

/**
 * SVG viewBox attribute: minX minY width height
 */
data class ViewBox(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float
)

/**
 * Base class for SVG elements
 */
sealed class SvgElement {
    abstract val transform: Transform?
}

/**
 * SVG <path> element
 */
data class PathElement(
    val pathData: String,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <g> group element
 */
data class GroupElement(
    val children: List<SvgElement>,
    val style: PathStyle?,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <circle> element
 */
data class CircleElement(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <ellipse> element
 */
data class EllipseElement(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <rect> element
 */
data class RectElement(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float,
    val ry: Float,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <line> element
 */
data class LineElement(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <polygon> element
 */
data class PolygonElement(
    val points: List<Pair<Float, Float>>,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <polyline> element
 */
data class PolylineElement(
    val points: List<Pair<Float, Float>>,
    val style: PathStyle,
    override val transform: Transform? = null
) : SvgElement()

/**
 * Path style properties (fill, stroke, opacity, etc.)
 */
data class PathStyle(
    val fill: SvgColor? = SvgColor.Black,
    val fillOpacity: Float = 1f,
    val stroke: SvgColor? = null,
    val strokeWidth: Float = 1f,
    val strokeOpacity: Float = 1f,
    val strokeLineCap: StrokeLineCap = StrokeLineCap.Butt,
    val strokeLineJoin: StrokeLineJoin = StrokeLineJoin.Miter,
    val strokeMiterLimit: Float = 4f,
    val opacity: Float = 1f,
    val clipPathId: String? = null,
    val maskId: String? = null,
    val filterId: String? = null
) {
    companion object {
        val Default = PathStyle()
        val None = PathStyle(fill = null)
    }
}

/**
 * SVG color representation
 */
sealed class SvgColor {
    /**
     * ARGB color value
     */
    data class Rgb(val argb: Long) : SvgColor()

    /**
     * No color (none/transparent)
     */
    data object None : SvgColor()

    /**
     * Current color (currentColor keyword)
     */
    data object CurrentColor : SvgColor()

    /**
     * Reference to a gradient or pattern by ID (url(#id))
     */
    data class Reference(val id: String) : SvgColor()

    companion object {
        val Black = Rgb(0xFF000000)
        val White = Rgb(0xFFFFFFFF)
        val Transparent = Rgb(0x00000000)
    }
}

/**
 * Stroke line cap style
 */
enum class StrokeLineCap {
    Butt,
    Round,
    Square
}

/**
 * Stroke line join style
 */
enum class StrokeLineJoin {
    Miter,
    Round,
    Bevel
}

/**
 * SVG transform
 */
sealed class Transform {
    /**
     * Translate transform
     */
    data class Translate(val x: Float, val y: Float) : Transform()

    /**
     * Scale transform
     */
    data class Scale(val x: Float, val y: Float = x) : Transform()

    /**
     * Rotate transform (degrees)
     */
    data class Rotate(val angle: Float, val cx: Float = 0f, val cy: Float = 0f) : Transform()

    /**
     * SkewX transform
     */
    data class SkewX(val angle: Float) : Transform()

    /**
     * SkewY transform
     */
    data class SkewY(val angle: Float) : Transform()

    /**
     * Matrix transform: matrix(a, b, c, d, e, f)
     */
    data class Matrix(
        val a: Float, val b: Float,
        val c: Float, val d: Float,
        val e: Float, val f: Float
    ) : Transform()

    /**
     * Multiple transforms combined
     */
    data class Combined(val transforms: List<Transform>) : Transform()
}

/**
 * SVG <defs> container for reusable elements
 */
data class SvgDefs(
    val gradients: Map<String, SvgGradient> = emptyMap(),
    val clipPaths: Map<String, ClipPath> = emptyMap(),
    val symbols: Map<String, SymbolElement> = emptyMap(),
    val masks: Map<String, Mask> = emptyMap(),
    val filters: Map<String, SvgFilter> = emptyMap(),
    val patterns: Map<String, SvgPattern> = emptyMap()
)

/**
 * Base class for SVG gradients
 */
sealed class SvgGradient {
    abstract val id: String
    abstract val stops: List<GradientStop>
    abstract val gradientUnits: GradientUnits
    abstract val spreadMethod: SpreadMethod
    abstract val gradientTransform: Transform?
}

/**
 * SVG <linearGradient> element
 */
data class LinearGradient(
    override val id: String,
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 1f,
    val y2: Float = 0f,
    override val stops: List<GradientStop>,
    override val gradientUnits: GradientUnits = GradientUnits.ObjectBoundingBox,
    override val spreadMethod: SpreadMethod = SpreadMethod.Pad,
    override val gradientTransform: Transform? = null
) : SvgGradient()

/**
 * SVG <radialGradient> element
 */
data class RadialGradient(
    override val id: String,
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val r: Float = 0.5f,
    val fx: Float = cx,
    val fy: Float = cy,
    override val stops: List<GradientStop>,
    override val gradientUnits: GradientUnits = GradientUnits.ObjectBoundingBox,
    override val spreadMethod: SpreadMethod = SpreadMethod.Pad,
    override val gradientTransform: Transform? = null
) : SvgGradient()

/**
 * Gradient color stop
 */
data class GradientStop(
    val offset: Float,
    val color: SvgColor.Rgb,
    val opacity: Float = 1f
)

/**
 * Gradient coordinate units
 */
enum class GradientUnits {
    UserSpaceOnUse,
    ObjectBoundingBox
}

/**
 * Gradient spread method
 */
enum class SpreadMethod {
    Pad,
    Reflect,
    Repeat
}

/**
 * SVG <clipPath> element
 */
data class ClipPath(
    val id: String,
    val elements: List<SvgElement>,
    val clipPathUnits: ClipPathUnits = ClipPathUnits.UserSpaceOnUse
)

/**
 * ClipPath coordinate units
 */
enum class ClipPathUnits {
    UserSpaceOnUse,
    ObjectBoundingBox
}

/**
 * SVG <mask> element
 */
data class Mask(
    val id: String,
    val elements: List<SvgElement>,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
    val maskUnits: MaskUnits = MaskUnits.ObjectBoundingBox,
    val maskContentUnits: MaskUnits = MaskUnits.UserSpaceOnUse
)

/**
 * Mask coordinate units
 */
enum class MaskUnits {
    UserSpaceOnUse,
    ObjectBoundingBox
}

/**
 * SVG <filter> element
 */
data class SvgFilter(
    val id: String,
    val x: Float = -0.1f,
    val y: Float = -0.1f,
    val width: Float = 1.2f,
    val height: Float = 1.2f,
    val filterUnits: FilterUnits = FilterUnits.ObjectBoundingBox,
    val primitives: List<FilterPrimitive> = emptyList()
)

/**
 * Filter coordinate units
 */
enum class FilterUnits {
    UserSpaceOnUse,
    ObjectBoundingBox
}

/**
 * Base class for filter primitives
 */
sealed class FilterPrimitive {
    abstract val result: String?
}

/**
 * feGaussianBlur filter primitive
 */
data class FeGaussianBlur(
    val input: String? = null,
    override val result: String? = null,
    val stdDeviationX: Float = 0f,
    val stdDeviationY: Float = stdDeviationX
) : FilterPrimitive()

/**
 * feOffset filter primitive
 */
data class FeOffset(
    val input: String? = null,
    override val result: String? = null,
    val dx: Float = 0f,
    val dy: Float = 0f
) : FilterPrimitive()

/**
 * feFlood filter primitive
 */
data class FeFlood(
    override val result: String? = null,
    val floodColor: SvgColor = SvgColor.Black,
    val floodOpacity: Float = 1f
) : FilterPrimitive()

/**
 * feBlend filter primitive
 */
data class FeBlend(
    val input: String? = null,
    val input2: String? = null,
    override val result: String? = null,
    val mode: BlendMode = BlendMode.Normal
) : FilterPrimitive()

/**
 * feComposite filter primitive
 */
data class FeComposite(
    val input: String? = null,
    val input2: String? = null,
    override val result: String? = null,
    val operator: CompositeOperator = CompositeOperator.Over
) : FilterPrimitive()

/**
 * feMerge filter primitive
 */
data class FeMerge(
    override val result: String? = null,
    val nodes: List<String> = emptyList()
) : FilterPrimitive()

/**
 * feColorMatrix filter primitive
 */
data class FeColorMatrix(
    val input: String? = null,
    override val result: String? = null,
    val type: ColorMatrixType = ColorMatrixType.Matrix,
    val values: List<Float> = emptyList()
) : FilterPrimitive()

/**
 * feDropShadow filter primitive (shorthand)
 */
data class FeDropShadow(
    val input: String? = null,
    override val result: String? = null,
    val dx: Float = 2f,
    val dy: Float = 2f,
    val stdDeviation: Float = 2f,
    val floodColor: SvgColor = SvgColor.Black,
    val floodOpacity: Float = 1f
) : FilterPrimitive()

/**
 * Blend mode for feBlend
 */
enum class BlendMode {
    Normal, Multiply, Screen, Overlay, Darken, Lighten,
    ColorDodge, ColorBurn, HardLight, SoftLight, Difference, Exclusion
}

/**
 * Composite operator for feComposite
 */
enum class CompositeOperator {
    Over, In, Out, Atop, Xor, Arithmetic
}

/**
 * Color matrix type for feColorMatrix
 */
enum class ColorMatrixType {
    Matrix, Saturate, HueRotate, LuminanceToAlpha
}

/**
 * SVG <pattern> element
 */
data class SvgPattern(
    val id: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val patternUnits: PatternUnits = PatternUnits.ObjectBoundingBox,
    val patternContentUnits: PatternUnits = PatternUnits.UserSpaceOnUse,
    val patternTransform: Transform? = null,
    val elements: List<SvgElement> = emptyList(),
    val viewBox: ViewBox? = null
)

/**
 * Pattern coordinate units
 */
enum class PatternUnits {
    UserSpaceOnUse,
    ObjectBoundingBox
}

/**
 * SVG <image> element
 */
data class ImageElement(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val href: String,
    val preserveAspectRatio: String = "xMidYMid meet",
    val style: PathStyle = PathStyle.Default,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <symbol> element (reusable graphic)
 */
data class SymbolElement(
    val id: String,
    val viewBox: ViewBox?,
    val elements: List<SvgElement>
)

/**
 * SVG <use> element (reference to a symbol or other element)
 */
data class UseElement(
    val href: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float? = null,
    val height: Float? = null,
    val style: PathStyle? = null,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <text> element
 */
data class TextElement(
    val x: Float = 0f,
    val y: Float = 0f,
    val text: String,
    val spans: List<TSpan> = emptyList(),
    val style: PathStyle = PathStyle.Default,
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 16f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal,
    val textAnchor: TextAnchor = TextAnchor.Start,
    val dominantBaseline: DominantBaseline = DominantBaseline.Auto,
    override val transform: Transform? = null
) : SvgElement()

/**
 * SVG <tspan> element - text span with different styling
 */
data class TSpan(
    val x: Float? = null,
    val y: Float? = null,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val text: String,
    val style: PathStyle? = null,
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val textAnchor: TextAnchor? = null,
    val dominantBaseline: DominantBaseline? = null
)

/**
 * Font weight values
 */
enum class FontWeight(val value: Int) {
    Normal(400),
    Bold(700),
    W100(100),
    W200(200),
    W300(300),
    W400(400),
    W500(500),
    W600(600),
    W700(700),
    W800(800),
    W900(900)
}

/**
 * Font style
 */
enum class FontStyle {
    Normal,
    Italic,
    Oblique
}

/**
 * Text anchor (horizontal alignment)
 */
enum class TextAnchor {
    Start,
    Middle,
    End
}

/**
 * Dominant baseline (vertical alignment)
 */
enum class DominantBaseline {
    Auto,
    Middle,
    Hanging,
    Central,
    TextTop,
    TextBottom,
    Alphabetic,
    Ideographic,
    Mathematical
}
