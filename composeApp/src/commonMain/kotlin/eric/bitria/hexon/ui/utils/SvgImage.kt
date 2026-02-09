@file:OptIn(ExperimentalEncodingApi::class)

package eric.bitria.hexon.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import io.aimei.wk.model.CircleElement
import io.aimei.wk.model.ClipPath
import io.aimei.wk.model.DominantBaseline
import io.aimei.wk.model.EllipseElement
import io.aimei.wk.model.FontWeight
import io.aimei.wk.model.GradientUnits
import io.aimei.wk.model.GroupElement
import io.aimei.wk.model.ImageElement
import io.aimei.wk.model.LineElement
import io.aimei.wk.model.PathElement
import io.aimei.wk.model.PathStyle
import io.aimei.wk.model.PolygonElement
import io.aimei.wk.model.PolylineElement
import io.aimei.wk.model.RectElement
import io.aimei.wk.model.SpreadMethod
import io.aimei.wk.model.StrokeLineCap
import io.aimei.wk.model.StrokeLineJoin
import io.aimei.wk.model.SvgColor
import io.aimei.wk.model.SvgDocument
import io.aimei.wk.model.SvgElement
import io.aimei.wk.model.SvgGradient
import io.aimei.wk.model.TextAnchor
import io.aimei.wk.model.TextElement
import io.aimei.wk.model.Transform
import io.aimei.wk.model.UseElement
import io.aimei.wk.parser.SvgParser
import io.aimei.wk.parser.SvgPathParser
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.PI
import kotlin.math.tan
import androidx.compose.ui.text.font.FontStyle as ComposeFontStyle
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight

/**
 * Composable that renders an SVG from string content
 *
 * @param svgContent The SVG content as a string
 * @param modifier Modifier for the composable
 * @param contentDescription Accessibility description
 * @param tint Optional tint color to apply (overrides fill color)
 */
@Composable
fun SvgImage(
    svgContent: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null
) {
    val document = remember(svgContent) {
        SvgParser.parse(svgContent)
    }

    SvgImage(
        document = document,
        modifier = modifier,
        contentDescription = contentDescription,
        tint = tint
    )
}

/**
 * Composable that renders a parsed SvgDocument
 *
 * @param document The parsed SVG document
 * @param modifier Modifier for the composable
 * @param contentDescription Accessibility description
 * @param tint Optional tint color to apply
 */
@Composable
fun SvgImage(
    document: SvgDocument,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / document.viewBoxWidth
        val scaleY = canvasHeight / document.viewBoxHeight
        val scale = minOf(scaleX, scaleY)

        val offsetX = (canvasWidth - document.viewBoxWidth * scale) / 2f - document.viewBoxMinX * scale
        val offsetY = (canvasHeight - document.viewBoxHeight * scale) / 2f - document.viewBoxMinY * scale

        for (element in document.elements) {
            renderElement(element, document, scale, offsetX, offsetY, tint, null, textMeasurer)
        }
    }
}

/**
 * Composable that renders an SVG with specific size
 *
 * @param svgContent The SVG content as a string
 * @param size The size to render at
 * @param modifier Additional modifier
 * @param contentDescription Accessibility description
 * @param tint Optional tint color
 */
@Composable
fun SvgImage(
    svgContent: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null
) {
    SvgImage(
        svgContent = svgContent,
        modifier = modifier.size(size),
        contentDescription = contentDescription,
        tint = tint
    )
}

/**
 * Render an SVG element
 */
private fun DrawScope.renderElement(
    element: SvgElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?,
    textMeasurer: TextMeasurer
) {
    when (element) {
        is PathElement -> renderPath(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is GroupElement -> renderGroup(element, document, scale, offsetX, offsetY, tint, parentStyle, textMeasurer)
        is CircleElement -> renderCircle(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is EllipseElement -> renderEllipse(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is RectElement -> renderRect(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is LineElement -> renderLine(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is PolygonElement -> renderPolygon(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is PolylineElement -> renderPolyline(element, document, scale, offsetX, offsetY, tint, parentStyle)
        is UseElement -> renderUseElement(element, document, scale, offsetX, offsetY, tint, parentStyle, textMeasurer)
        is TextElement -> renderText(element, document, scale, offsetX, offsetY, tint, parentStyle, textMeasurer)
        is ImageElement -> renderImage(element, document, scale, offsetX, offsetY, tint, parentStyle)
    }
}

/**
 * Render a path element
 */
private fun DrawScope.renderPath(
    element: PathElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    val pathCommands = SvgPathParser.parse(element.pathData, scale, offsetX, offsetY)
    val path = PathRenderer.createPath(pathCommands)

    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val style = mergeStyle(element.style, parentStyle)
        drawStyledPath(path, style, document, tint)
    }
}

/**
 * Render a group element
 */
private fun DrawScope.renderGroup(
    element: GroupElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?,
    textMeasurer: TextMeasurer
) {
    val groupStyle = element.style?.let { mergeStyle(it, parentStyle) } ?: parentStyle

    withElementTransform(element.transform, scale, offsetX, offsetY) {
        for (child in element.children) {
            renderElement(child, document, scale, offsetX, offsetY, tint, groupStyle, textMeasurer)
        }
    }
}

/**
 * Render a circle element
 */
private fun DrawScope.renderCircle(
    element: CircleElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val center = Offset(
            element.cx * scale + offsetX,
            element.cy * scale + offsetY
        )
        val radius = element.r * scale
        val style = mergeStyle(element.style, parentStyle)

        // Create bounds for gradient calculation
        val bounds = Rect(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )

        // Draw fill
        getFillBrush(style, document, bounds, tint)?.let { brush ->
            drawCircle(
                brush = brush,
                radius = radius,
                center = center,
                alpha = style.opacity * style.fillOpacity
            )
        }

        // Draw stroke
        getStrokeBrush(style, document, bounds, tint)?.let { brush ->
            drawCircle(
                brush = brush,
                radius = radius,
                center = center,
                alpha = style.opacity * style.strokeOpacity,
                style = Stroke(
                    width = style.strokeWidth * scale,
                    cap = style.strokeLineCap.toCompose(),
                    join = style.strokeLineJoin.toCompose(),
                    miter = style.strokeMiterLimit
                )
            )
        }
    }
}

/**
 * Render an ellipse element
 */
private fun DrawScope.renderEllipse(
    element: EllipseElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val topLeft = Offset(
            (element.cx - element.rx) * scale + offsetX,
            (element.cy - element.ry) * scale + offsetY
        )
        val size = Size(element.rx * 2 * scale, element.ry * 2 * scale)
        val style = mergeStyle(element.style, parentStyle)
        val bounds = Rect(topLeft, size)

        // Draw fill
        getFillBrush(style, document, bounds, tint)?.let { brush ->
            drawOval(
                brush = brush,
                topLeft = topLeft,
                size = size,
                alpha = style.opacity * style.fillOpacity
            )
        }

        // Draw stroke
        getStrokeBrush(style, document, bounds, tint)?.let { brush ->
            drawOval(
                brush = brush,
                topLeft = topLeft,
                size = size,
                alpha = style.opacity * style.strokeOpacity,
                style = Stroke(
                    width = style.strokeWidth * scale,
                    cap = style.strokeLineCap.toCompose(),
                    join = style.strokeLineJoin.toCompose(),
                    miter = style.strokeMiterLimit
                )
            )
        }
    }
}

/**
 * Render a rect element
 */
private fun DrawScope.renderRect(
    element: RectElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val topLeft = Offset(
            element.x * scale + offsetX,
            element.y * scale + offsetY
        )
        val size = Size(element.width * scale, element.height * scale)
        val style = mergeStyle(element.style, parentStyle)
        val bounds = Rect(topLeft, size)

        val cornerRadiusX = element.rx * scale
        val cornerRadiusY = element.ry * scale

        // For rounded rectangles, use path
        if (cornerRadiusX > 0 || cornerRadiusY > 0) {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = topLeft.x,
                        top = topLeft.y,
                        right = topLeft.x + size.width,
                        bottom = topLeft.y + size.height,
                        radiusX = cornerRadiusX,
                        radiusY = cornerRadiusY
                    )
                )
            }
            drawStyledPath(path, style, document, tint)
        } else {
            // Draw fill
            getFillBrush(style, document, bounds, tint)?.let { brush ->
                drawRect(
                    brush = brush,
                    topLeft = topLeft,
                    size = size,
                    alpha = style.opacity * style.fillOpacity
                )
            }

            // Draw stroke
            getStrokeBrush(style, document, bounds, tint)?.let { brush ->
                drawRect(
                    brush = brush,
                    topLeft = topLeft,
                    size = size,
                    alpha = style.opacity * style.strokeOpacity,
                    style = Stroke(
                        width = style.strokeWidth * scale,
                        cap = style.strokeLineCap.toCompose(),
                        join = style.strokeLineJoin.toCompose(),
                        miter = style.strokeMiterLimit
                    )
                )
            }
        }
    }
}

/**
 * Render a line element
 */
private fun DrawScope.renderLine(
    element: LineElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val start = Offset(
            element.x1 * scale + offsetX,
            element.y1 * scale + offsetY
        )
        val end = Offset(
            element.x2 * scale + offsetX,
            element.y2 * scale + offsetY
        )
        val style = mergeStyle(element.style, parentStyle)

        getStrokeColor(style, tint)?.let { color ->
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = style.strokeWidth * scale,
                cap = style.strokeLineCap.toCompose(),
                alpha = style.opacity * style.strokeOpacity
            )
        }
    }
}

/**
 * Render a polygon element
 */
private fun DrawScope.renderPolygon(
    element: PolygonElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    if (element.points.isEmpty()) return

    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val path = Path().apply {
            val first = element.points.first()
            moveTo(first.first * scale + offsetX, first.second * scale + offsetY)
            for (i in 1 until element.points.size) {
                val point = element.points[i]
                lineTo(point.first * scale + offsetX, point.second * scale + offsetY)
            }
            close()
        }

        val style = mergeStyle(element.style, parentStyle)
        drawStyledPath(path, style, document, tint)
    }
}

/**
 * Render a polyline element
 */
private fun DrawScope.renderPolyline(
    element: PolylineElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    if (element.points.isEmpty()) return

    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val path = Path().apply {
            val first = element.points.first()
            moveTo(first.first * scale + offsetX, first.second * scale + offsetY)
            for (i in 1 until element.points.size) {
                val point = element.points[i]
                lineTo(point.first * scale + offsetX, point.second * scale + offsetY)
            }
        }

        val style = mergeStyle(element.style, parentStyle)
        drawStyledPath(path, style, document, tint)
    }
}

/**
 * Render a use element (reference to symbol or other element)
 */
private fun DrawScope.renderUseElement(
    element: UseElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?,
    textMeasurer: TextMeasurer
) {
    val refId = element.href
    val symbol = document.defs.symbols[refId] ?: return

    val useStyle = element.style?.let { mergeStyle(it, parentStyle) } ?: parentStyle

    // Apply use element transform and position
    val combinedTransform: Transform? = if (element.x != 0f || element.y != 0f) {
        val translateTransform = Transform.Translate(element.x, element.y)
        if (element.transform != null) {
            Transform.Combined(listOf(translateTransform, element.transform!!))
        } else {
            translateTransform
        }
    } else {
        element.transform
    }

    withElementTransform(combinedTransform, scale, offsetX, offsetY) {
        for (child in symbol.elements) {
            renderElement(child, document, scale, offsetX, offsetY, tint, useStyle, textMeasurer)
        }
    }
}

/**
 * Render a text element using Compose's cross-platform text API
 */
private fun DrawScope.renderText(
    element: TextElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?,
    textMeasurer: TextMeasurer
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val style = mergeStyle(element.style, parentStyle)
        val baseX = element.x * scale + offsetX
        val baseY = element.y * scale + offsetY
        val fontSizeInPx = element.fontSize * scale
        val fontSizeInSp = (fontSizeInPx / density).sp

        val fillColor = when (val fill = style.fill) {
            is SvgColor.Rgb -> tint ?: Color(fill.argb.toInt())
            is SvgColor.CurrentColor -> tint ?: Color.Black
            else -> tint ?: Color.Black
        }

        val baseTextStyle = TextStyle(
            color = fillColor,
            fontSize = fontSizeInSp,
            fontWeight = element.fontWeight.toCompose(),
            fontStyle = element.fontStyle.toCompose()
        )

        // Build the entire text as a single AnnotatedString so that
        // text-anchor and baseline apply to the whole run.
        val annotatedString = buildAnnotatedString {
            if (element.spans.isNotEmpty()) {
                // Leading text before first tspan
                if (element.text.isNotEmpty()) {
                    withStyle(SpanStyle(
                        color = fillColor,
                        fontSize = fontSizeInSp,
                        fontWeight = element.fontWeight.toCompose(),
                        fontStyle = element.fontStyle.toCompose()
                    )) {
                        append(element.text)
                    }
                }
                // Tspan children
                for (span in element.spans) {
                    val spanFontSizeInPx = (span.fontSize ?: element.fontSize) * scale
                    val spanFontSizeInSp = (spanFontSizeInPx / density).sp
                    val spanFillColor = when (val spanFill = span.style?.fill ?: style.fill) {
                        is SvgColor.Rgb -> tint ?: Color(spanFill.argb.toInt())
                        is SvgColor.CurrentColor -> tint ?: Color.Black
                        else -> tint ?: Color.Black
                    }
                    withStyle(SpanStyle(
                        color = spanFillColor,
                        fontSize = spanFontSizeInSp,
                        fontWeight = (span.fontWeight ?: element.fontWeight).toCompose(),
                        fontStyle = (span.fontStyle ?: element.fontStyle).toCompose()
                    )) {
                        append(span.text)
                    }
                }
            } else {
                append(element.text)
            }
        }

        val textLayoutResult = textMeasurer.measure(annotatedString, baseTextStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat()
        val baseline = textLayoutResult.firstBaseline

        val adjustedX = when (element.textAnchor) {
            TextAnchor.Middle -> baseX - textWidth / 2
            TextAnchor.End -> baseX - textWidth
            TextAnchor.Start -> baseX
        }
        val adjustedY = when (element.dominantBaseline) {
            DominantBaseline.Middle, DominantBaseline.Central -> baseY - textHeight / 2
            DominantBaseline.Hanging, DominantBaseline.TextTop -> baseY
            DominantBaseline.Alphabetic, DominantBaseline.Auto -> baseY - baseline
            DominantBaseline.Ideographic, DominantBaseline.TextBottom -> baseY - textHeight
            DominantBaseline.Mathematical -> baseY - baseline * 0.7f
        }

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(adjustedX, adjustedY)
        )
    }
}

/**
 * Render an image element
 * Note: Image rendering requires loading the image data from the href.
 * For data URIs (base64), we decode and render directly.
 * For external URLs, the image would need to be loaded asynchronously (not implemented yet).
 */
private fun DrawScope.renderImage(
    element: ImageElement,
    document: SvgDocument,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    tint: Color?,
    parentStyle: PathStyle?
) {
    withElementTransform(element.transform, scale, offsetX, offsetY) {
        val x = element.x * scale + offsetX
        val y = element.y * scale + offsetY
        val width = element.width * scale
        val height = element.height * scale

        // Handle data URI images (base64 encoded)
        if (element.href.startsWith("data:image/")) {
            // Note: Full image rendering requires platform-specific implementation
            // For now, decode the data and draw a placeholder
            // TODO: Add expect/actual for platform-specific image decoding
            try {
                val base64Data = element.href.substringAfter("base64,")
                val imageBytes = Base64.decode(base64Data)
                // Image bytes decoded successfully, but rendering requires platform-specific code
                // Draw a placeholder with indicator that image data exists
                drawRect(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
            } catch (e: Exception) {
                // If decoding fails, draw a placeholder rectangle
                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
            }
        } else {
            // External URLs not supported in synchronous rendering
            // Draw a placeholder rectangle
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(x, y),
                size = Size(width, height)
            )
        }
    }
}

/**
 * Draw a path with fill and stroke, applying clipPath if specified
 */
private fun DrawScope.drawStyledPath(
    path: Path,
    style: PathStyle,
    document: SvgDocument,
    tint: Color?,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    val bounds = path.getBounds()

    // Get clipPath if specified
    val clipPathDef = style.clipPathId?.let { document.defs.clipPaths[it] }

    // Helper to draw the actual content
    val drawContent: DrawScope.() -> Unit = {
        // Draw fill
        getFillBrush(style, document, bounds, tint)?.let { brush ->
            drawPath(
                path = path,
                brush = brush,
                alpha = style.opacity * style.fillOpacity
            )
        }

        // Draw stroke
        getStrokeBrush(style, document, bounds, tint)?.let { brush ->
            drawPath(
                path = path,
                brush = brush,
                alpha = style.opacity * style.strokeOpacity,
                style = Stroke(
                    width = style.strokeWidth,
                    cap = style.strokeLineCap.toCompose(),
                    join = style.strokeLineJoin.toCompose(),
                    miter = style.strokeMiterLimit
                )
            )
        }
    }

    // Apply clipPath if present
    if (clipPathDef != null) {
        val clipPathPath = buildClipPath(clipPathDef, scale, offsetX, offsetY)
        clipPath(clipPathPath) {
            drawContent()
        }
    } else {
        drawContent()
    }
}

/**
 * Build a Path from ClipPath definition
 */
private fun buildClipPath(
    clipPathDef: ClipPath,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Path {
    val combinedPath = Path()

    for (element in clipPathDef.elements) {
        when (element) {
            is PathElement -> {
                val pathCommands = SvgPathParser.parse(element.pathData, scale, offsetX, offsetY)
                val elementPath = PathRenderer.createPath(pathCommands)
                combinedPath.addPath(elementPath)
            }
            is RectElement -> {
                val topLeft = Offset(
                    element.x * scale + offsetX,
                    element.y * scale + offsetY
                )
                val size = Size(element.width * scale, element.height * scale)
                if (element.rx > 0 || element.ry > 0) {
                    combinedPath.addRoundRect(
                        RoundRect(
                            left = topLeft.x,
                            top = topLeft.y,
                            right = topLeft.x + size.width,
                            bottom = topLeft.y + size.height,
                            radiusX = element.rx * scale,
                            radiusY = element.ry * scale
                        )
                    )
                } else {
                    combinedPath.addRect(Rect(topLeft, size))
                }
            }
            is CircleElement -> {
                val center = Offset(
                    element.cx * scale + offsetX,
                    element.cy * scale + offsetY
                )
                val radius = element.r * scale
                combinedPath.addOval(Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ))
            }
            is EllipseElement -> {
                val topLeft = Offset(
                    (element.cx - element.rx) * scale + offsetX,
                    (element.cy - element.ry) * scale + offsetY
                )
                val size = Size(element.rx * 2 * scale, element.ry * 2 * scale)
                combinedPath.addOval(Rect(topLeft, size))
            }
            is PolygonElement -> {
                if (element.points.isNotEmpty()) {
                    val first = element.points.first()
                    combinedPath.moveTo(first.first * scale + offsetX, first.second * scale + offsetY)
                    for (i in 1 until element.points.size) {
                        val point = element.points[i]
                        combinedPath.lineTo(point.first * scale + offsetX, point.second * scale + offsetY)
                    }
                    combinedPath.close()
                }
            }
            else -> {} // Other elements not typically used in clipPath
        }
    }

    return combinedPath
}

/**
 * Apply element transform
 */
private inline fun DrawScope.withElementTransform(
    transform: Transform?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    block: DrawScope.() -> Unit
) {
    if (transform == null) {
        block()
    } else {
        withTransform({
            applyTransform(transform, scale, offsetX, offsetY)
        }) {
            block()
        }
    }
}

/**
 * Apply a Transform to the current transform matrix
 */
private fun DrawTransform.applyTransform(
    transform: Transform,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    when (transform) {
        is Transform.Translate -> {
            translate(transform.x * scale, transform.y * scale)
        }
        is Transform.Scale -> {
            scale(transform.x, transform.y)
        }
        is Transform.Rotate -> {
            rotate(
                degrees = transform.angle,
                pivot = Offset(
                    transform.cx * scale + offsetX,
                    transform.cy * scale + offsetY
                )
            )
        }
        is Transform.SkewX -> {
            val radians = transform.angle * PI.toFloat() / 180f
            transform(Matrix().apply {
                values[Matrix.SkewX] = tan(radians)
            })
        }
        is Transform.SkewY -> {
            val radians = transform.angle * PI.toFloat() / 180f
            transform(Matrix().apply {
                values[Matrix.SkewY] = tan(radians)
            })
        }
        is Transform.Matrix -> {
            transform(Matrix().apply {
                values[Matrix.ScaleX] = transform.a
                values[Matrix.SkewY] = transform.b
                values[Matrix.SkewX] = transform.c
                values[Matrix.ScaleY] = transform.d
                values[Matrix.TranslateX] = transform.e * scale
                values[Matrix.TranslateY] = transform.f * scale
            })
        }
        is Transform.Combined -> {
            for (t in transform.transforms) {
                applyTransform(t, scale, offsetX, offsetY)
            }
        }
    }
}

/**
 * Get fill brush (supports gradients and solid colors)
 */
private fun getFillBrush(style: PathStyle, document: SvgDocument, bounds: Rect, tint: Color?): Brush? {
    if (tint != null) return SolidColor(tint)

    return when (val fill = style.fill) {
        is SvgColor.Rgb -> SolidColor(Color(fill.argb.toInt()))
        is SvgColor.None -> null
        is SvgColor.CurrentColor -> SolidColor(Color.Black)
        is SvgColor.Reference -> {
            val gradient = document.defs.gradients[fill.id]
            gradient?.toBrush(bounds)
        }
        null -> null
    }
}

/**
 * Get stroke brush (supports gradients and solid colors)
 */
private fun getStrokeBrush(style: PathStyle, document: SvgDocument, bounds: Rect, tint: Color?): Brush? {
    return when (val stroke = style.stroke) {
        is SvgColor.Rgb -> SolidColor(if (tint != null) tint else Color(stroke.argb.toInt()))
        is SvgColor.None -> null
        is SvgColor.CurrentColor -> SolidColor(tint ?: Color.Black)
        is SvgColor.Reference -> {
            val gradient = document.defs.gradients[stroke.id]
            gradient?.toBrush(bounds)
        }
        null -> null
    }
}

/**
 * Get fill color (for simple cases without gradient support)
 */
private fun getFillColor(style: PathStyle, tint: Color?): Color? {
    if (tint != null) return tint

    return when (val fill = style.fill) {
        is SvgColor.Rgb -> Color(fill.argb.toInt())
        is SvgColor.None -> null
        is SvgColor.CurrentColor -> Color.Black
        is SvgColor.Reference -> null // Gradients not supported for this method
        null -> null
    }
}

/**
 * Get stroke color (for simple cases without gradient support)
 */
private fun getStrokeColor(style: PathStyle, tint: Color?): Color? {
    return when (val stroke = style.stroke) {
        is SvgColor.Rgb -> tint ?: Color(stroke.argb.toInt())
        is SvgColor.None -> null
        is SvgColor.CurrentColor -> tint ?: Color.Black
        is SvgColor.Reference -> null // Gradients not supported for this method
        null -> null
    }
}

/**
 * Convert SvgGradient to Compose Brush
 */
private fun SvgGradient.toBrush(bounds: Rect): Brush {
    val colorStops = stops.map { stop ->
        val color = Color(stop.color.argb.toInt()).copy(alpha = stop.opacity)
        stop.offset to color
    }.toTypedArray()

    return when (this) {
        is io.aimei.wk.model.LinearGradient -> {
            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float

            if (gradientUnits == GradientUnits.ObjectBoundingBox) {
                // Coordinates are relative to bounding box (0-1)
                startX = bounds.left + this.x1 * bounds.width
                startY = bounds.top + this.y1 * bounds.height
                endX = bounds.left + this.x2 * bounds.width
                endY = bounds.top + this.y2 * bounds.height
            } else {
                // Coordinates are in user space
                startX = this.x1
                startY = this.y1
                endX = this.x2
                endY = this.y2
            }

            Brush.linearGradient(
                colorStops = colorStops,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                tileMode = spreadMethod.toTileMode()
            )
        }
        is io.aimei.wk.model.RadialGradient -> {
            val centerX: Float
            val centerY: Float
            val radius: Float

            if (gradientUnits == GradientUnits.ObjectBoundingBox) {
                centerX = bounds.left + this.cx * bounds.width
                centerY = bounds.top + this.cy * bounds.height
                radius = this.r * maxOf(bounds.width, bounds.height)
            } else {
                centerX = this.cx
                centerY = this.cy
                radius = this.r
            }

            Brush.radialGradient(
                colorStops = colorStops,
                center = Offset(centerX, centerY),
                radius = radius,
                tileMode = spreadMethod.toTileMode()
            )
        }
    }
}

/**
 * Convert SpreadMethod to Compose TileMode
 */
private fun SpreadMethod.toTileMode(): TileMode = when (this) {
    SpreadMethod.Pad -> TileMode.Clamp
    SpreadMethod.Reflect -> TileMode.Mirror
    SpreadMethod.Repeat -> TileMode.Repeated
}

/**
 * Merge child style with parent style
 */
private fun mergeStyle(childStyle: PathStyle, parentStyle: PathStyle?): PathStyle {
    if (parentStyle == null) return childStyle

    return PathStyle(
        fill = childStyle.fill ?: parentStyle.fill,
        fillOpacity = if (childStyle.fillOpacity != 1f) childStyle.fillOpacity else parentStyle.fillOpacity,
        stroke = childStyle.stroke ?: parentStyle.stroke,
        strokeWidth = if (childStyle.strokeWidth != 1f) childStyle.strokeWidth else parentStyle.strokeWidth,
        strokeOpacity = if (childStyle.strokeOpacity != 1f) childStyle.strokeOpacity else parentStyle.strokeOpacity,
        strokeLineCap = childStyle.strokeLineCap,
        strokeLineJoin = childStyle.strokeLineJoin,
        strokeMiterLimit = if (childStyle.strokeMiterLimit != 4f) childStyle.strokeMiterLimit else parentStyle.strokeMiterLimit,
        opacity = childStyle.opacity * parentStyle.opacity,
        clipPathId = childStyle.clipPathId ?: parentStyle.clipPathId,
        maskId = childStyle.maskId ?: parentStyle.maskId,
        filterId = childStyle.filterId ?: parentStyle.filterId
    )
}

/**
 * Convert StrokeLineCap to Compose StrokeCap
 */
private fun StrokeLineCap.toCompose(): StrokeCap = when (this) {
    StrokeLineCap.Butt -> StrokeCap.Butt
    StrokeLineCap.Round -> StrokeCap.Round
    StrokeLineCap.Square -> StrokeCap.Square
}

/**
 * Convert StrokeLineJoin to Compose StrokeJoin
 */
private fun StrokeLineJoin.toCompose(): StrokeJoin = when (this) {
    StrokeLineJoin.Miter -> StrokeJoin.Miter
    StrokeLineJoin.Round -> StrokeJoin.Round
    StrokeLineJoin.Bevel -> StrokeJoin.Bevel
}

/**
 * Convert model FontWeight to Compose FontWeight
 */
private fun FontWeight.toCompose(): ComposeFontWeight = when (this) {
    FontWeight.Normal -> ComposeFontWeight.Normal
    FontWeight.Bold -> ComposeFontWeight.Bold
    FontWeight.W100 -> ComposeFontWeight.W100
    FontWeight.W200 -> ComposeFontWeight.W200
    FontWeight.W300 -> ComposeFontWeight.W300
    FontWeight.W400 -> ComposeFontWeight.W400
    FontWeight.W500 -> ComposeFontWeight.W500
    FontWeight.W600 -> ComposeFontWeight.W600
    FontWeight.W700 -> ComposeFontWeight.W700
    FontWeight.W800 -> ComposeFontWeight.W800
    FontWeight.W900 -> ComposeFontWeight.W900
}

/**
 * Convert model FontStyle to Compose FontStyle
 */
private fun io.aimei.wk.model.FontStyle.toCompose(): ComposeFontStyle = when (this) {
    io.aimei.wk.model.FontStyle.Normal -> ComposeFontStyle.Normal
    io.aimei.wk.model.FontStyle.Italic -> ComposeFontStyle.Italic
    io.aimei.wk.model.FontStyle.Oblique -> ComposeFontStyle.Italic // Compose doesn't have Oblique, use Italic
}