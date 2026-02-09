package io.aimei.wk.parser

import io.aimei.wk.model.*

/**
 * SVG document parser
 * Parses SVG XML content into SvgDocument model
 */
object SvgParser {

    /**
     * Parse SVG content string into SvgDocument
     */
    fun parse(svgContent: String): SvgDocument {
        val width = parseAttribute(svgContent, "width")?.toFloatOrNull() ?: 512f
        val height = parseAttribute(svgContent, "height")?.toFloatOrNull() ?: 512f
        val viewBox = parseViewBox(svgContent)
        val defs = parseDefs(svgContent)
        val elements = parseElements(svgContent)

        return SvgDocument(
            width = width,
            height = height,
            viewBox = viewBox,
            elements = elements,
            defs = defs
        )
    }

    /**
     * Parse viewBox attribute: "minX minY width height"
     */
    private fun parseViewBox(content: String): ViewBox? {
        val viewBoxAttr = parseAttribute(content, "viewBox") ?: return null
        val parts = viewBoxAttr.trim().split(Regex("[\\s,]+"))
        if (parts.size != 4) return null

        return try {
            ViewBox(
                minX = parts[0].toFloat(),
                minY = parts[1].toFloat(),
                width = parts[2].toFloat(),
                height = parts[3].toFloat()
            )
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Parse <defs> section for gradients, clipPaths, symbols, masks, filters, and patterns
     */
    private fun parseDefs(content: String): SvgDefs {
        val defsRegex = Regex("""<defs[^>]*>([\s\S]*?)</defs>""")
        val defsContent = defsRegex.find(content)?.groupValues?.get(1) ?: ""

        // Also look for gradients outside of defs (some SVGs put them at root level)
        val allContent = defsContent + content

        val gradients = mutableMapOf<String, SvgGradient>()
        val clipPaths = mutableMapOf<String, ClipPath>()
        val symbols = mutableMapOf<String, SymbolElement>()
        val masks = mutableMapOf<String, Mask>()
        val filters = mutableMapOf<String, SvgFilter>()
        val patterns = mutableMapOf<String, SvgPattern>()

        // Parse linearGradient
        parseLinearGradients(allContent).forEach { gradients[it.id] = it }

        // Parse radialGradient
        parseRadialGradients(allContent).forEach { gradients[it.id] = it }

        // Parse clipPath
        parseClipPaths(defsContent).forEach { clipPaths[it.id] = it }

        // Parse symbols
        parseSymbols(defsContent).forEach { symbols[it.id] = it }

        // Parse masks
        parseMasks(defsContent).forEach { masks[it.id] = it }

        // Parse filters
        parseFilters(defsContent).forEach { filters[it.id] = it }

        // Parse patterns
        parsePatterns(defsContent).forEach { patterns[it.id] = it }

        return SvgDefs(
            gradients = gradients,
            clipPaths = clipPaths,
            symbols = symbols,
            masks = masks,
            filters = filters,
            patterns = patterns
        )
    }

    /**
     * Parse <linearGradient> elements
     */
    private fun parseLinearGradients(content: String): List<LinearGradient> {
        val gradients = mutableListOf<LinearGradient>()
        // Match both self-closing and regular tags
        val gradientRegex = Regex(
            """<linearGradient([^>]*)(?:>([\s\S]*?)</linearGradient>|/>)"""
        )

        gradientRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues.getOrNull(2) ?: ""

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val x1 = parseGradientCoord(parseAttribute(attributes, "x1"), 0f)
            val y1 = parseGradientCoord(parseAttribute(attributes, "y1"), 0f)
            val x2 = parseGradientCoord(parseAttribute(attributes, "x2"), 1f)
            val y2 = parseGradientCoord(parseAttribute(attributes, "y2"), 0f)
            val gradientUnits = parseGradientUnits(parseAttribute(attributes, "gradientUnits"))
            val spreadMethod = parseSpreadMethod(parseAttribute(attributes, "spreadMethod"))
            val gradientTransform = parseTransform(attributes)

            val stops = parseGradientStops(innerContent)

            gradients.add(LinearGradient(
                id = id,
                x1 = x1,
                y1 = y1,
                x2 = x2,
                y2 = y2,
                stops = stops,
                gradientUnits = gradientUnits,
                spreadMethod = spreadMethod,
                gradientTransform = gradientTransform
            ))
        }

        return gradients
    }

    /**
     * Parse <radialGradient> elements
     */
    private fun parseRadialGradients(content: String): List<RadialGradient> {
        val gradients = mutableListOf<RadialGradient>()
        val gradientRegex = Regex(
            """<radialGradient([^>]*)(?:>([\s\S]*?)</radialGradient>|/>)"""
        )

        gradientRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues.getOrNull(2) ?: ""

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val cx = parseGradientCoord(parseAttribute(attributes, "cx"), 0.5f)
            val cy = parseGradientCoord(parseAttribute(attributes, "cy"), 0.5f)
            val r = parseGradientCoord(parseAttribute(attributes, "r"), 0.5f)
            val fx = parseGradientCoord(parseAttribute(attributes, "fx"), cx)
            val fy = parseGradientCoord(parseAttribute(attributes, "fy"), cy)
            val gradientUnits = parseGradientUnits(parseAttribute(attributes, "gradientUnits"))
            val spreadMethod = parseSpreadMethod(parseAttribute(attributes, "spreadMethod"))
            val gradientTransform = parseTransform(attributes)

            val stops = parseGradientStops(innerContent)

            gradients.add(RadialGradient(
                id = id,
                cx = cx,
                cy = cy,
                r = r,
                fx = fx,
                fy = fy,
                stops = stops,
                gradientUnits = gradientUnits,
                spreadMethod = spreadMethod,
                gradientTransform = gradientTransform
            ))
        }

        return gradients
    }

    /**
     * Parse gradient coordinate value (handles percentage and absolute)
     */
    private fun parseGradientCoord(value: String?, default: Float): Float {
        if (value == null) return default
        val trimmed = value.trim()
        return if (trimmed.endsWith("%")) {
            trimmed.dropLast(1).toFloatOrNull()?.div(100f) ?: default
        } else {
            trimmed.toFloatOrNull() ?: default
        }
    }

    /**
     * Parse <stop> elements within a gradient
     */
    private fun parseGradientStops(content: String): List<GradientStop> {
        val stops = mutableListOf<GradientStop>()
        val stopRegex = Regex("""<stop([^>]*)/?>""")

        stopRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]

            // Parse offset
            val offsetStr = parseAttribute(attributes, "offset") ?: "0"
            val offset = if (offsetStr.endsWith("%")) {
                offsetStr.dropLast(1).toFloatOrNull()?.div(100f) ?: 0f
            } else {
                offsetStr.toFloatOrNull() ?: 0f
            }

            // Parse stop-color (from attribute or style)
            val styleAttr = parseAttribute(attributes, "style")
            val stopColorFromStyle = styleAttr?.let { style ->
                style.split(";").find { it.trim().startsWith("stop-color") }
                    ?.substringAfter(":")?.trim()
            }
            val stopColorStr = parseAttribute(attributes, "stop-color") ?: stopColorFromStyle ?: "#000000"
            val stopColor = parseColor(stopColorStr) as? SvgColor.Rgb ?: SvgColor.Black as SvgColor.Rgb

            // Parse stop-opacity
            val stopOpacityFromStyle = styleAttr?.let { style ->
                style.split(";").find { it.trim().startsWith("stop-opacity") }
                    ?.substringAfter(":")?.trim()?.toFloatOrNull()
            }
            val stopOpacity = parseAttribute(attributes, "stop-opacity")?.toFloatOrNull()
                ?: stopOpacityFromStyle ?: 1f

            stops.add(GradientStop(
                offset = offset,
                color = stopColor,
                opacity = stopOpacity
            ))
        }

        return stops.sortedBy { it.offset }
    }

    /**
     * Parse gradientUnits attribute
     */
    private fun parseGradientUnits(value: String?): GradientUnits {
        return when (value?.lowercase()) {
            "userspaceonuse" -> GradientUnits.UserSpaceOnUse
            else -> GradientUnits.ObjectBoundingBox
        }
    }

    /**
     * Parse spreadMethod attribute
     */
    private fun parseSpreadMethod(value: String?): SpreadMethod {
        return when (value?.lowercase()) {
            "reflect" -> SpreadMethod.Reflect
            "repeat" -> SpreadMethod.Repeat
            else -> SpreadMethod.Pad
        }
    }

    /**
     * Parse <clipPath> elements
     */
    private fun parseClipPaths(content: String): List<ClipPath> {
        val clipPaths = mutableListOf<ClipPath>()
        val clipPathRegex = Regex("""<clipPath([^>]*)>([\s\S]*?)</clipPath>""")

        clipPathRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val clipPathUnits = when (parseAttribute(attributes, "clipPathUnits")?.lowercase()) {
                "objectboundingbox" -> ClipPathUnits.ObjectBoundingBox
                else -> ClipPathUnits.UserSpaceOnUse
            }
            val elements = parseElements(innerContent)

            clipPaths.add(ClipPath(
                id = id,
                elements = elements,
                clipPathUnits = clipPathUnits
            ))
        }

        return clipPaths
    }

    /**
     * Parse <symbol> elements
     */
    private fun parseSymbols(content: String): List<SymbolElement> {
        val symbols = mutableListOf<SymbolElement>()
        val symbolRegex = Regex("""<symbol([^>]*)>([\s\S]*?)</symbol>""")

        symbolRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val viewBox = parseViewBox(attributes)
            val elements = parseElements(innerContent)

            symbols.add(SymbolElement(
                id = id,
                viewBox = viewBox,
                elements = elements
            ))
        }

        return symbols
    }

    /**
     * Parse <mask> elements
     */
    private fun parseMasks(content: String): List<Mask> {
        val masks = mutableListOf<Mask>()
        val maskRegex = Regex("""<mask([^>]*)>([\s\S]*?)</mask>""")

        maskRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull() ?: 1f
            val height = parseAttribute(attributes, "height")?.toFloatOrNull() ?: 1f
            val maskUnits = when (parseAttribute(attributes, "maskUnits")?.lowercase()) {
                "userspaceonuse" -> MaskUnits.UserSpaceOnUse
                else -> MaskUnits.ObjectBoundingBox
            }
            val maskContentUnits = when (parseAttribute(attributes, "maskContentUnits")?.lowercase()) {
                "objectboundingbox" -> MaskUnits.ObjectBoundingBox
                else -> MaskUnits.UserSpaceOnUse
            }
            val elements = parseElements(innerContent)

            masks.add(Mask(
                id = id,
                elements = elements,
                x = x,
                y = y,
                width = width,
                height = height,
                maskUnits = maskUnits,
                maskContentUnits = maskContentUnits
            ))
        }

        return masks
    }

    /**
     * Parse <filter> elements
     */
    private fun parseFilters(content: String): List<SvgFilter> {
        val filters = mutableListOf<SvgFilter>()
        val filterRegex = Regex("""<filter([^>]*)>([\s\S]*?)</filter>""")

        filterRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: -0.1f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: -0.1f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull() ?: 1.2f
            val height = parseAttribute(attributes, "height")?.toFloatOrNull() ?: 1.2f
            val filterUnits = when (parseAttribute(attributes, "filterUnits")?.lowercase()) {
                "userspaceonuse" -> FilterUnits.UserSpaceOnUse
                else -> FilterUnits.ObjectBoundingBox
            }
            val primitives = parseFilterPrimitives(innerContent)

            filters.add(SvgFilter(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                filterUnits = filterUnits,
                primitives = primitives
            ))
        }

        return filters
    }

    /**
     * Parse filter primitives within a <filter> element
     */
    private fun parseFilterPrimitives(content: String): List<FilterPrimitive> {
        val primitives = mutableListOf<FilterPrimitive>()

        // feGaussianBlur
        Regex("""<feGaussianBlur([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            val stdDeviation = parseAttribute(attrs, "stdDeviation")?.split(Regex("[\\s,]+"))
            val stdDeviationX = stdDeviation?.getOrNull(0)?.toFloatOrNull() ?: 0f
            val stdDeviationY = stdDeviation?.getOrNull(1)?.toFloatOrNull() ?: stdDeviationX

            primitives.add(FeGaussianBlur(
                input = parseAttribute(attrs, "in"),
                result = parseAttribute(attrs, "result"),
                stdDeviationX = stdDeviationX,
                stdDeviationY = stdDeviationY
            ))
        }

        // feOffset
        Regex("""<feOffset([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            primitives.add(FeOffset(
                input = parseAttribute(attrs, "in"),
                result = parseAttribute(attrs, "result"),
                dx = parseAttribute(attrs, "dx")?.toFloatOrNull() ?: 0f,
                dy = parseAttribute(attrs, "dy")?.toFloatOrNull() ?: 0f
            ))
        }

        // feFlood
        Regex("""<feFlood([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            val floodColor = parseColor(parseAttribute(attrs, "flood-color")) ?: SvgColor.Black
            primitives.add(FeFlood(
                result = parseAttribute(attrs, "result"),
                floodColor = floodColor,
                floodOpacity = parseAttribute(attrs, "flood-opacity")?.toFloatOrNull() ?: 1f
            ))
        }

        // feBlend
        Regex("""<feBlend([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            primitives.add(FeBlend(
                input = parseAttribute(attrs, "in"),
                input2 = parseAttribute(attrs, "in2"),
                result = parseAttribute(attrs, "result"),
                mode = parseBlendMode(parseAttribute(attrs, "mode"))
            ))
        }

        // feComposite
        Regex("""<feComposite([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            primitives.add(FeComposite(
                input = parseAttribute(attrs, "in"),
                input2 = parseAttribute(attrs, "in2"),
                result = parseAttribute(attrs, "result"),
                operator = parseCompositeOperator(parseAttribute(attrs, "operator"))
            ))
        }

        // feMerge
        Regex("""<feMerge([^>]*)>([\s\S]*?)</feMerge>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            val innerContent = match.groupValues[2]
            val nodes = Regex("""<feMergeNode[^>]*in\s*=\s*["']([^"']+)["'][^>]*/?>""")
                .findAll(innerContent)
                .map { it.groupValues[1] }
                .toList()

            primitives.add(FeMerge(
                result = parseAttribute(attrs, "result"),
                nodes = nodes
            ))
        }

        // feColorMatrix
        Regex("""<feColorMatrix([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            val type = when (parseAttribute(attrs, "type")?.lowercase()) {
                "saturate" -> ColorMatrixType.Saturate
                "huerotate" -> ColorMatrixType.HueRotate
                "luminancetoalpha" -> ColorMatrixType.LuminanceToAlpha
                else -> ColorMatrixType.Matrix
            }
            val values = parseAttribute(attrs, "values")
                ?.split(Regex("[\\s,]+"))
                ?.mapNotNull { it.toFloatOrNull() } ?: emptyList()

            primitives.add(FeColorMatrix(
                input = parseAttribute(attrs, "in"),
                result = parseAttribute(attrs, "result"),
                type = type,
                values = values
            ))
        }

        // feDropShadow
        Regex("""<feDropShadow([^>]*)/?>""").findAll(content).forEach { match ->
            val attrs = match.groupValues[1]
            val floodColor = parseColor(parseAttribute(attrs, "flood-color")) ?: SvgColor.Black

            primitives.add(FeDropShadow(
                input = parseAttribute(attrs, "in"),
                result = parseAttribute(attrs, "result"),
                dx = parseAttribute(attrs, "dx")?.toFloatOrNull() ?: 2f,
                dy = parseAttribute(attrs, "dy")?.toFloatOrNull() ?: 2f,
                stdDeviation = parseAttribute(attrs, "stdDeviation")?.toFloatOrNull() ?: 2f,
                floodColor = floodColor,
                floodOpacity = parseAttribute(attrs, "flood-opacity")?.toFloatOrNull() ?: 1f
            ))
        }

        return primitives
    }

    /**
     * Parse blend mode for feBlend
     */
    private fun parseBlendMode(value: String?): BlendMode {
        return when (value?.lowercase()) {
            "multiply" -> BlendMode.Multiply
            "screen" -> BlendMode.Screen
            "overlay" -> BlendMode.Overlay
            "darken" -> BlendMode.Darken
            "lighten" -> BlendMode.Lighten
            "color-dodge" -> BlendMode.ColorDodge
            "color-burn" -> BlendMode.ColorBurn
            "hard-light" -> BlendMode.HardLight
            "soft-light" -> BlendMode.SoftLight
            "difference" -> BlendMode.Difference
            "exclusion" -> BlendMode.Exclusion
            else -> BlendMode.Normal
        }
    }

    /**
     * Parse composite operator for feComposite
     */
    private fun parseCompositeOperator(value: String?): CompositeOperator {
        return when (value?.lowercase()) {
            "in" -> CompositeOperator.In
            "out" -> CompositeOperator.Out
            "atop" -> CompositeOperator.Atop
            "xor" -> CompositeOperator.Xor
            "arithmetic" -> CompositeOperator.Arithmetic
            else -> CompositeOperator.Over
        }
    }

    /**
     * Parse <pattern> elements
     */
    private fun parsePatterns(content: String): List<SvgPattern> {
        val patterns = mutableListOf<SvgPattern>()
        val patternRegex = Regex("""<pattern([^>]*)>([\s\S]*?)</pattern>""")

        patternRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val id = parseAttribute(attributes, "id") ?: return@forEach
            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull() ?: 0f
            val height = parseAttribute(attributes, "height")?.toFloatOrNull() ?: 0f
            val patternUnits = when (parseAttribute(attributes, "patternUnits")?.lowercase()) {
                "userspaceonuse" -> PatternUnits.UserSpaceOnUse
                else -> PatternUnits.ObjectBoundingBox
            }
            val patternContentUnits = when (parseAttribute(attributes, "patternContentUnits")?.lowercase()) {
                "objectboundingbox" -> PatternUnits.ObjectBoundingBox
                else -> PatternUnits.UserSpaceOnUse
            }
            val patternTransform = parseTransform(attributes)
            val viewBox = parseViewBox(attributes)
            val elements = parseElements(innerContent)

            patterns.add(SvgPattern(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                patternUnits = patternUnits,
                patternContentUnits = patternContentUnits,
                patternTransform = patternTransform,
                elements = elements,
                viewBox = viewBox
            ))
        }

        return patterns
    }

    /**
     * Parse all SVG elements from content
     */
    private fun parseElements(content: String): List<SvgElement> {
        val elements = mutableListOf<SvgElement>()

        // Parse <g> groups first (recursive)
        elements.addAll(parseGroups(content))

        // Remove group content to avoid parsing nested elements at top level
        val contentWithoutGroups = removeGroupContent(content)

        // Parse <path> elements (only those not in groups)
        elements.addAll(parsePaths(contentWithoutGroups))

        // Parse basic shapes (only those not in groups)
        elements.addAll(parseCircles(contentWithoutGroups))
        elements.addAll(parseEllipses(contentWithoutGroups))
        elements.addAll(parseRects(contentWithoutGroups))
        elements.addAll(parseLines(contentWithoutGroups))
        elements.addAll(parsePolygons(contentWithoutGroups))
        elements.addAll(parsePolylines(contentWithoutGroups))

        // Parse <use> references
        elements.addAll(parseUseElements(contentWithoutGroups))

        // Parse <text> elements
        elements.addAll(parseTextElements(contentWithoutGroups))

        // Parse <image> elements
        elements.addAll(parseImageElements(contentWithoutGroups))

        return elements
    }

    /**
     * Remove <g>...</g>, <defs>...</defs>, <symbol>...</symbol> content from string to avoid double-parsing
     */
    private fun removeGroupContent(content: String): String {
        var result = content
        result = result.replace(Regex("""<g[^>]*>[\s\S]*?</g>"""), "")
        result = result.replace(Regex("""<defs[^>]*>[\s\S]*?</defs>"""), "")
        result = result.replace(Regex("""<symbol[^>]*>[\s\S]*?</symbol>"""), "")
        return result
    }

    /**
     * Parse <use> elements
     */
    private fun parseUseElements(content: String): List<UseElement> {
        val uses = mutableListOf<UseElement>()
        val useRegex = Regex("""<use([^>]+)/?>""")

        useRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]

            // Parse href (xlink:href or href)
            val href = parseAttribute(attributes, "href")
                ?: parseAttribute(attributes, "xlink:href")
                ?: return@forEach

            // Remove leading # from href
            val refId = if (href.startsWith("#")) href.substring(1) else href

            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull()
            val height = parseAttribute(attributes, "height")?.toFloatOrNull()
            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            uses.add(UseElement(
                href = refId,
                x = x,
                y = y,
                width = width,
                height = height,
                style = style,
                transform = transform
            ))
        }

        return uses
    }

    /**
     * Parse <g> group elements
     */
    private fun parseGroups(content: String): List<GroupElement> {
        val groups = mutableListOf<GroupElement>()
        val groupRegex = Regex("""<g([^>]*)>([\s\S]*?)</g>""")

        groupRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)
            val children = parseElements(innerContent)

            groups.add(GroupElement(
                children = children,
                style = style,
                transform = transform
            ))
        }

        return groups
    }

    /**
     * Parse <path> elements
     */
    private fun parsePaths(content: String): List<PathElement> {
        val paths = mutableListOf<PathElement>()
        val pathRegex = Regex("""<path([^>]+)/?>""")

        pathRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val d = parseAttribute(attributes, "d") ?: return@forEach

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            paths.add(PathElement(
                pathData = d,
                style = style,
                transform = transform
            ))
        }

        return paths
    }

    /**
     * Parse <circle> elements
     */
    private fun parseCircles(content: String): List<CircleElement> {
        val circles = mutableListOf<CircleElement>()
        val circleRegex = Regex("""<circle([^>]+)/?>""")

        circleRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val cx = parseAttribute(attributes, "cx")?.toFloatOrNull() ?: 0f
            val cy = parseAttribute(attributes, "cy")?.toFloatOrNull() ?: 0f
            val r = parseAttribute(attributes, "r")?.toFloatOrNull() ?: return@forEach

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            circles.add(CircleElement(
                cx = cx,
                cy = cy,
                r = r,
                style = style,
                transform = transform
            ))
        }

        return circles
    }

    /**
     * Parse <ellipse> elements
     */
    private fun parseEllipses(content: String): List<EllipseElement> {
        val ellipses = mutableListOf<EllipseElement>()
        val ellipseRegex = Regex("""<ellipse([^>]+)/?>""")

        ellipseRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val cx = parseAttribute(attributes, "cx")?.toFloatOrNull() ?: 0f
            val cy = parseAttribute(attributes, "cy")?.toFloatOrNull() ?: 0f
            val rx = parseAttribute(attributes, "rx")?.toFloatOrNull() ?: return@forEach
            val ry = parseAttribute(attributes, "ry")?.toFloatOrNull() ?: return@forEach

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            ellipses.add(EllipseElement(
                cx = cx,
                cy = cy,
                rx = rx,
                ry = ry,
                style = style,
                transform = transform
            ))
        }

        return ellipses
    }

    /**
     * Parse <rect> elements
     */
    private fun parseRects(content: String): List<RectElement> {
        val rects = mutableListOf<RectElement>()
        val rectRegex = Regex("""<rect([^>]+)/?>""")

        rectRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull() ?: return@forEach
            val height = parseAttribute(attributes, "height")?.toFloatOrNull() ?: return@forEach
            val rx = parseAttribute(attributes, "rx")?.toFloatOrNull() ?: 0f
            val ry = parseAttribute(attributes, "ry")?.toFloatOrNull() ?: rx

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            rects.add(RectElement(
                x = x,
                y = y,
                width = width,
                height = height,
                rx = rx,
                ry = ry,
                style = style,
                transform = transform
            ))
        }

        return rects
    }

    /**
     * Parse <line> elements
     */
    private fun parseLines(content: String): List<LineElement> {
        val lines = mutableListOf<LineElement>()
        val lineRegex = Regex("""<line([^>]+)/?>""")

        lineRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val x1 = parseAttribute(attributes, "x1")?.toFloatOrNull() ?: 0f
            val y1 = parseAttribute(attributes, "y1")?.toFloatOrNull() ?: 0f
            val x2 = parseAttribute(attributes, "x2")?.toFloatOrNull() ?: 0f
            val y2 = parseAttribute(attributes, "y2")?.toFloatOrNull() ?: 0f

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            lines.add(LineElement(
                x1 = x1,
                y1 = y1,
                x2 = x2,
                y2 = y2,
                style = style,
                transform = transform
            ))
        }

        return lines
    }

    /**
     * Parse <polygon> elements
     */
    private fun parsePolygons(content: String): List<PolygonElement> {
        val polygons = mutableListOf<PolygonElement>()
        val polygonRegex = Regex("""<polygon([^>]+)/?>""")

        polygonRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val points = parsePoints(parseAttribute(attributes, "points") ?: return@forEach)

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            polygons.add(PolygonElement(
                points = points,
                style = style,
                transform = transform
            ))
        }

        return polygons
    }

    /**
     * Parse <polyline> elements
     */
    private fun parsePolylines(content: String): List<PolylineElement> {
        val polylines = mutableListOf<PolylineElement>()
        val polylineRegex = Regex("""<polyline([^>]+)/?>""")

        polylineRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val points = parsePoints(parseAttribute(attributes, "points") ?: return@forEach)

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            polylines.add(PolylineElement(
                points = points,
                style = style,
                transform = transform
            ))
        }

        return polylines
    }

    /**
     * Parse points attribute "x1,y1 x2,y2 ..."
     */
    private fun parsePoints(pointsAttr: String): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val numbers = pointsAttr.trim().split(Regex("[\\s,]+"))

        var i = 0
        while (i + 1 < numbers.size) {
            val x = numbers[i].toFloatOrNull()
            val y = numbers[i + 1].toFloatOrNull()
            if (x != null && y != null) {
                points.add(x to y)
            }
            i += 2
        }

        return points
    }

    /**
     * Parse style attributes from element
     */
    private fun parseStyle(attributes: String): PathStyle {
        val fill = parseColor(parseAttribute(attributes, "fill"))
        val fillOpacity = parseAttribute(attributes, "fill-opacity")?.toFloatOrNull() ?: 1f
        val stroke = parseColor(parseAttribute(attributes, "stroke"))
        val strokeWidth = parseAttribute(attributes, "stroke-width")?.toFloatOrNull() ?: 1f
        val strokeOpacity = parseAttribute(attributes, "stroke-opacity")?.toFloatOrNull() ?: 1f
        val strokeLineCap = parseStrokeLineCap(parseAttribute(attributes, "stroke-linecap"))
        val strokeLineJoin = parseStrokeLineJoin(parseAttribute(attributes, "stroke-linejoin"))
        val strokeMiterLimit = parseAttribute(attributes, "stroke-miterlimit")?.toFloatOrNull() ?: 4f
        val opacity = parseAttribute(attributes, "opacity")?.toFloatOrNull() ?: 1f

        // Parse clip-path, mask, filter references
        val clipPathId = parseUrlReference(parseAttribute(attributes, "clip-path"))
        val maskId = parseUrlReference(parseAttribute(attributes, "mask"))
        val filterId = parseUrlReference(parseAttribute(attributes, "filter"))

        // Also parse style attribute (CSS inline style)
        val styleAttr = parseAttribute(attributes, "style")
        val cssStyle = styleAttr?.let { parseCssStyle(it) }

        return PathStyle(
            fill = cssStyle?.fill ?: fill ?: SvgColor.Black,
            fillOpacity = cssStyle?.fillOpacity ?: fillOpacity,
            stroke = cssStyle?.stroke ?: stroke,
            strokeWidth = cssStyle?.strokeWidth ?: strokeWidth,
            strokeOpacity = cssStyle?.strokeOpacity ?: strokeOpacity,
            strokeLineCap = cssStyle?.strokeLineCap ?: strokeLineCap,
            strokeLineJoin = cssStyle?.strokeLineJoin ?: strokeLineJoin,
            strokeMiterLimit = cssStyle?.strokeMiterLimit ?: strokeMiterLimit,
            opacity = cssStyle?.opacity ?: opacity,
            clipPathId = cssStyle?.clipPathId ?: clipPathId,
            maskId = cssStyle?.maskId ?: maskId,
            filterId = cssStyle?.filterId ?: filterId
        )
    }

    /**
     * Parse url(#id) reference
     */
    private fun parseUrlReference(value: String?): String? {
        if (value == null) return null
        val urlRegex = Regex("""url\(#([^)]+)\)""")
        return urlRegex.find(value.trim())?.groupValues?.get(1)
    }

    /**
     * Parse CSS inline style attribute
     */
    private fun parseCssStyle(style: String): PathStyle? {
        val properties = style.split(";").associate { prop ->
            val parts = prop.split(":")
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                "" to ""
            }
        }

        if (properties.isEmpty()) return null

        return PathStyle(
            fill = parseColor(properties["fill"]),
            fillOpacity = properties["fill-opacity"]?.toFloatOrNull() ?: 1f,
            stroke = parseColor(properties["stroke"]),
            strokeWidth = properties["stroke-width"]?.replace("px", "")?.toFloatOrNull() ?: 1f,
            strokeOpacity = properties["stroke-opacity"]?.toFloatOrNull() ?: 1f,
            strokeLineCap = parseStrokeLineCap(properties["stroke-linecap"]),
            strokeLineJoin = parseStrokeLineJoin(properties["stroke-linejoin"]),
            strokeMiterLimit = properties["stroke-miterlimit"]?.toFloatOrNull() ?: 4f,
            opacity = properties["opacity"]?.toFloatOrNull() ?: 1f,
            clipPathId = parseUrlReference(properties["clip-path"]),
            maskId = parseUrlReference(properties["mask"]),
            filterId = parseUrlReference(properties["filter"])
        )
    }

    /**
     * Parse color value
     */
    private fun parseColor(value: String?): SvgColor? {
        if (value == null) return null

        val trimmed = value.trim()

        // Check for url(#id) reference to gradient or pattern
        if (trimmed.startsWith("url(")) {
            val urlRegex = Regex("""url\(#([^)]+)\)""")
            val match = urlRegex.find(trimmed)
            if (match != null) {
                return SvgColor.Reference(match.groupValues[1])
            }
        }

        return when (trimmed.lowercase()) {
            "none", "transparent" -> SvgColor.None
            "currentcolor" -> SvgColor.CurrentColor
            "black" -> SvgColor.Black
            "white" -> SvgColor.White
            else -> parseHexColor(trimmed) ?: parseNamedColor(trimmed)
        }
    }

    /**
     * Parse hex color: #RGB, #RRGGBB, #AARRGGBB
     */
    private fun parseHexColor(value: String): SvgColor? {
        if (!value.startsWith("#")) return null

        val hex = value.substring(1)
        return try {
            val argb = when (hex.length) {
                3 -> {
                    val r = hex[0].toString().repeat(2)
                    val g = hex[1].toString().repeat(2)
                    val b = hex[2].toString().repeat(2)
                    "FF$r$g$b".toLong(16)
                }
                6 -> "FF$hex".toLong(16)
                8 -> hex.toLong(16)
                else -> return null
            }
            SvgColor.Rgb(argb)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Parse named colors
     */
    private fun parseNamedColor(name: String): SvgColor? {
        return NAMED_COLORS[name.lowercase()]?.let { SvgColor.Rgb(it) }
    }

    /**
     * Parse stroke-linecap attribute
     */
    private fun parseStrokeLineCap(value: String?): StrokeLineCap {
        return when (value?.lowercase()) {
            "round" -> StrokeLineCap.Round
            "square" -> StrokeLineCap.Square
            else -> StrokeLineCap.Butt
        }
    }

    /**
     * Parse stroke-linejoin attribute
     */
    private fun parseStrokeLineJoin(value: String?): StrokeLineJoin {
        return when (value?.lowercase()) {
            "round" -> StrokeLineJoin.Round
            "bevel" -> StrokeLineJoin.Bevel
            else -> StrokeLineJoin.Miter
        }
    }

    /**
     * Parse transform attribute
     */
    private fun parseTransform(attributes: String): Transform? {
        val transformAttr = parseAttribute(attributes, "transform") ?: return null
        return TransformParser.parse(transformAttr)
    }

    /**
     * Parse a single attribute value from attribute string
     */
    private fun parseAttribute(attributes: String, name: String): String? {
        val regex = Regex("""$name\s*=\s*["']([^"']+)["']""")
        return regex.find(attributes)?.groupValues?.get(1)
    }

    /**
     * Parse <text> elements
     */
    private fun parseTextElements(content: String): List<TextElement> {
        val texts = mutableListOf<TextElement>()
        // Match <text ...>content</text> including content with tspan
        val textRegex = Regex("""<text([^>]*)>([\s\S]*?)</text>""")

        textRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]
            val innerContent = match.groupValues[2]

            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f

            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            // Parse font attributes
            val fontFamily = parseAttribute(attributes, "font-family") ?: "sans-serif"
            val fontSize = parseAttribute(attributes, "font-size")?.replace("px", "")?.toFloatOrNull() ?: 16f
            val fontWeight = parseFontWeight(parseAttribute(attributes, "font-weight"))
            val fontStyle = parseFontStyle(parseAttribute(attributes, "font-style"))
            val textAnchor = parseTextAnchor(parseAttribute(attributes, "text-anchor"))
            val dominantBaseline = parseDominantBaseline(parseAttribute(attributes, "dominant-baseline"))

            // Also check CSS style attribute for font properties
            val styleAttr = parseAttribute(attributes, "style")
            val cssProps = styleAttr?.let { parseCssProperties(it) } ?: emptyMap()

            // Parse tspan elements with interleaved text
            val (textContent, spans) = parseTextContentAndSpans(innerContent)

            // Skip if no text content and no spans
            if (textContent.isEmpty() && spans.isEmpty()) return@forEach

            texts.add(TextElement(
                x = x,
                y = y,
                text = textContent,
                spans = spans,
                style = style,
                fontFamily = cssProps["font-family"] ?: fontFamily,
                fontSize = cssProps["font-size"]?.replace("px", "")?.toFloatOrNull() ?: fontSize,
                fontWeight = cssProps["font-weight"]?.let { parseFontWeight(it) } ?: fontWeight,
                fontStyle = cssProps["font-style"]?.let { parseFontStyle(it) } ?: fontStyle,
                textAnchor = cssProps["text-anchor"]?.let { parseTextAnchor(it) } ?: textAnchor,
                dominantBaseline = cssProps["dominant-baseline"]?.let { parseDominantBaseline(it) } ?: dominantBaseline,
                transform = transform
            ))
        }

        return texts
    }

    /**
     * Parse text content and tspan elements, handling interleaved text properly.
     * Returns: Pair of (text before first tspan, list of TSpan including synthetic spans for text between/after tspans)
     */
    private fun parseTextContentAndSpans(content: String): Pair<String, List<TSpan>> {
        val tspanRegex = Regex("""<tspan([^>]*)>([\s\S]*?)</tspan>""")
        val matches = tspanRegex.findAll(content).toList()

        if (matches.isEmpty()) {
            // No tspans, just return the text content
            return Pair(content.trim(), emptyList())
        }

        val spans = mutableListOf<TSpan>()
        var lastEnd = 0

        // Text before first tspan
        val textBeforeFirstTspan = content.substring(0, matches.first().range.first).trim()

        for ((index, match) in matches.withIndex()) {
            // Check for text between previous tspan and this one
            if (index > 0) {
                val textBetween = content.substring(lastEnd, match.range.first).trim()
                if (textBetween.isNotEmpty()) {
                    // Create a synthetic TSpan for the text between
                    spans.add(TSpan(text = textBetween))
                }
            }

            // Parse this tspan
            val attributes = match.groupValues[1]
            val textContent = match.groupValues[2].trim()

            if (textContent.isNotEmpty()) {
                val x = parseAttribute(attributes, "x")?.toFloatOrNull()
                val y = parseAttribute(attributes, "y")?.toFloatOrNull()
                val dx = parseAttribute(attributes, "dx")?.toFloatOrNull() ?: 0f
                val dy = parseAttribute(attributes, "dy")?.toFloatOrNull() ?: 0f

                val style = if (parseAttribute(attributes, "fill") != null ||
                    parseAttribute(attributes, "stroke") != null ||
                    parseAttribute(attributes, "style") != null) {
                    parseStyle(attributes)
                } else null

                // Parse font attributes
                val fontFamily = parseAttribute(attributes, "font-family")
                val fontSize = parseAttribute(attributes, "font-size")?.replace("px", "")?.toFloatOrNull()
                val fontWeight = parseAttribute(attributes, "font-weight")?.let { parseFontWeight(it) }
                val fontStyle = parseAttribute(attributes, "font-style")?.let { parseFontStyle(it) }
                val textAnchor = parseAttribute(attributes, "text-anchor")?.let { parseTextAnchor(it) }
                val dominantBaseline = parseAttribute(attributes, "dominant-baseline")?.let { parseDominantBaseline(it) }

                // Also check CSS style attribute for font properties
                val styleAttr = parseAttribute(attributes, "style")
                val cssProps = styleAttr?.let { parseCssProperties(it) } ?: emptyMap()

                spans.add(TSpan(
                    x = x,
                    y = y,
                    dx = dx,
                    dy = dy,
                    text = textContent,
                    style = style,
                    fontFamily = cssProps["font-family"] ?: fontFamily,
                    fontSize = cssProps["font-size"]?.replace("px", "")?.toFloatOrNull() ?: fontSize,
                    fontWeight = cssProps["font-weight"]?.let { parseFontWeight(it) } ?: fontWeight,
                    fontStyle = cssProps["font-style"]?.let { parseFontStyle(it) } ?: fontStyle,
                    textAnchor = cssProps["text-anchor"]?.let { parseTextAnchor(it) } ?: textAnchor,
                    dominantBaseline = cssProps["dominant-baseline"]?.let { parseDominantBaseline(it) } ?: dominantBaseline
                ))
            }

            lastEnd = match.range.last + 1
        }

        // Check for text after last tspan
        if (lastEnd < content.length) {
            val textAfter = content.substring(lastEnd).trim()
            if (textAfter.isNotEmpty()) {
                spans.add(TSpan(text = textAfter))
            }
        }

        return Pair(textBeforeFirstTspan, spans)
    }

    /**
     * Parse <image> elements
     */
    private fun parseImageElements(content: String): List<ImageElement> {
        val images = mutableListOf<ImageElement>()
        val imageRegex = Regex("""<image([^>]+)/?>""")

        imageRegex.findAll(content).forEach { match ->
            val attributes = match.groupValues[1]

            // Parse href (xlink:href or href)
            val href = parseAttribute(attributes, "href")
                ?: parseAttribute(attributes, "xlink:href")
                ?: return@forEach

            val x = parseAttribute(attributes, "x")?.toFloatOrNull() ?: 0f
            val y = parseAttribute(attributes, "y")?.toFloatOrNull() ?: 0f
            val width = parseAttribute(attributes, "width")?.toFloatOrNull() ?: 0f
            val height = parseAttribute(attributes, "height")?.toFloatOrNull() ?: 0f
            val preserveAspectRatio = parseAttribute(attributes, "preserveAspectRatio") ?: "xMidYMid meet"
            val style = parseStyle(attributes)
            val transform = parseTransform(attributes)

            images.add(ImageElement(
                x = x,
                y = y,
                width = width,
                height = height,
                href = href,
                preserveAspectRatio = preserveAspectRatio,
                style = style,
                transform = transform
            ))
        }

        return images
    }

    /**
     * Parse CSS properties from style attribute
     */
    private fun parseCssProperties(style: String): Map<String, String> {
        return style.split(";").mapNotNull { prop ->
            val parts = prop.split(":")
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                null
            }
        }.toMap()
    }

    /**
     * Parse font-weight attribute
     */
    private fun parseFontWeight(value: String?): FontWeight {
        if (value == null) return FontWeight.Normal
        return when (value.lowercase()) {
            "normal" -> FontWeight.Normal
            "bold" -> FontWeight.Bold
            "100" -> FontWeight.W100
            "200" -> FontWeight.W200
            "300" -> FontWeight.W300
            "400" -> FontWeight.W400
            "500" -> FontWeight.W500
            "600" -> FontWeight.W600
            "700" -> FontWeight.W700
            "800" -> FontWeight.W800
            "900" -> FontWeight.W900
            else -> FontWeight.Normal
        }
    }

    /**
     * Parse font-style attribute
     */
    private fun parseFontStyle(value: String?): FontStyle {
        if (value == null) return FontStyle.Normal
        return when (value.lowercase()) {
            "italic" -> FontStyle.Italic
            "oblique" -> FontStyle.Oblique
            else -> FontStyle.Normal
        }
    }

    /**
     * Parse text-anchor attribute
     */
    private fun parseTextAnchor(value: String?): TextAnchor {
        if (value == null) return TextAnchor.Start
        return when (value.lowercase()) {
            "middle" -> TextAnchor.Middle
            "end" -> TextAnchor.End
            else -> TextAnchor.Start
        }
    }

    /**
     * Parse dominant-baseline attribute
     */
    private fun parseDominantBaseline(value: String?): DominantBaseline {
        if (value == null) return DominantBaseline.Auto
        return when (value.lowercase()) {
            "middle" -> DominantBaseline.Middle
            "hanging" -> DominantBaseline.Hanging
            "central" -> DominantBaseline.Central
            "text-top" -> DominantBaseline.TextTop
            "text-bottom" -> DominantBaseline.TextBottom
            "alphabetic" -> DominantBaseline.Alphabetic
            "ideographic" -> DominantBaseline.Ideographic
            "mathematical" -> DominantBaseline.Mathematical
            else -> DominantBaseline.Auto
        }
    }

    /**
     * Common named colors
     */
    private val NAMED_COLORS = mapOf(
        "aliceblue" to 0xFFF0F8FFL,
        "antiquewhite" to 0xFFFAEBD7L,
        "aqua" to 0xFF00FFFFL,
        "aquamarine" to 0xFF7FFFD4L,
        "azure" to 0xFFF0FFFFL,
        "beige" to 0xFFF5F5DCL,
        "bisque" to 0xFFFFE4C4L,
        "black" to 0xFF000000L,
        "blanchedalmond" to 0xFFFFEBCDL,
        "blue" to 0xFF0000FFL,
        "blueviolet" to 0xFF8A2BE2L,
        "brown" to 0xFFA52A2AL,
        "burlywood" to 0xFFDEB887L,
        "cadetblue" to 0xFF5F9EA0L,
        "chartreuse" to 0xFF7FFF00L,
        "chocolate" to 0xFFD2691EL,
        "coral" to 0xFFFF7F50L,
        "cornflowerblue" to 0xFF6495EDL,
        "cornsilk" to 0xFFFFF8DCL,
        "crimson" to 0xFFDC143CL,
        "cyan" to 0xFF00FFFFL,
        "darkblue" to 0xFF00008BL,
        "darkcyan" to 0xFF008B8BL,
        "darkgoldenrod" to 0xFFB8860BL,
        "darkgray" to 0xFFA9A9A9L,
        "darkgreen" to 0xFF006400L,
        "darkgrey" to 0xFFA9A9A9L,
        "darkkhaki" to 0xFFBDB76BL,
        "darkmagenta" to 0xFF8B008BL,
        "darkolivegreen" to 0xFF556B2FL,
        "darkorange" to 0xFFFF8C00L,
        "darkorchid" to 0xFF9932CCL,
        "darkred" to 0xFF8B0000L,
        "darksalmon" to 0xFFE9967AL,
        "darkseagreen" to 0xFF8FBC8FL,
        "darkslateblue" to 0xFF483D8BL,
        "darkslategray" to 0xFF2F4F4FL,
        "darkslategrey" to 0xFF2F4F4FL,
        "darkturquoise" to 0xFF00CED1L,
        "darkviolet" to 0xFF9400D3L,
        "deeppink" to 0xFFFF1493L,
        "deepskyblue" to 0xFF00BFFFL,
        "dimgray" to 0xFF696969L,
        "dimgrey" to 0xFF696969L,
        "dodgerblue" to 0xFF1E90FFL,
        "firebrick" to 0xFFB22222L,
        "floralwhite" to 0xFFFFFAF0L,
        "forestgreen" to 0xFF228B22L,
        "fuchsia" to 0xFFFF00FFL,
        "gainsboro" to 0xFFDCDCDCL,
        "ghostwhite" to 0xFFF8F8FFL,
        "gold" to 0xFFFFD700L,
        "goldenrod" to 0xFFDAA520L,
        "gray" to 0xFF808080L,
        "green" to 0xFF008000L,
        "greenyellow" to 0xFFADFF2FL,
        "grey" to 0xFF808080L,
        "honeydew" to 0xFFF0FFF0L,
        "hotpink" to 0xFFFF69B4L,
        "indianred" to 0xFFCD5C5CL,
        "indigo" to 0xFF4B0082L,
        "ivory" to 0xFFFFFFF0L,
        "khaki" to 0xFFF0E68CL,
        "lavender" to 0xFFE6E6FAL,
        "lavenderblush" to 0xFFFFF0F5L,
        "lawngreen" to 0xFF7CFC00L,
        "lemonchiffon" to 0xFFFFFACDL,
        "lightblue" to 0xFFADD8E6L,
        "lightcoral" to 0xFFF08080L,
        "lightcyan" to 0xFFE0FFFFL,
        "lightgoldenrodyellow" to 0xFFFAFAD2L,
        "lightgray" to 0xFFD3D3D3L,
        "lightgreen" to 0xFF90EE90L,
        "lightgrey" to 0xFFD3D3D3L,
        "lightpink" to 0xFFFFB6C1L,
        "lightsalmon" to 0xFFFFA07AL,
        "lightseagreen" to 0xFF20B2AAL,
        "lightskyblue" to 0xFF87CEFAL,
        "lightslategray" to 0xFF778899L,
        "lightslategrey" to 0xFF778899L,
        "lightsteelblue" to 0xFFB0C4DEL,
        "lightyellow" to 0xFFFFFFE0L,
        "lime" to 0xFF00FF00L,
        "limegreen" to 0xFF32CD32L,
        "linen" to 0xFFFAF0E6L,
        "magenta" to 0xFFFF00FFL,
        "maroon" to 0xFF800000L,
        "mediumaquamarine" to 0xFF66CDAAL,
        "mediumblue" to 0xFF0000CDL,
        "mediumorchid" to 0xFFBA55D3L,
        "mediumpurple" to 0xFF9370DBL,
        "mediumseagreen" to 0xFF3CB371L,
        "mediumslateblue" to 0xFF7B68EEL,
        "mediumspringgreen" to 0xFF00FA9AL,
        "mediumturquoise" to 0xFF48D1CCL,
        "mediumvioletred" to 0xFFC71585L,
        "midnightblue" to 0xFF191970L,
        "mintcream" to 0xFFF5FFFAL,
        "mistyrose" to 0xFFFFE4E1L,
        "moccasin" to 0xFFFFE4B5L,
        "navajowhite" to 0xFFFFDEADL,
        "navy" to 0xFF000080L,
        "oldlace" to 0xFFFDF5E6L,
        "olive" to 0xFF808000L,
        "olivedrab" to 0xFF6B8E23L,
        "orange" to 0xFFFFA500L,
        "orangered" to 0xFFFF4500L,
        "orchid" to 0xFFDA70D6L,
        "palegoldenrod" to 0xFFEEE8AAL,
        "palegreen" to 0xFF98FB98L,
        "paleturquoise" to 0xFFAFEEEEL,
        "palevioletred" to 0xFFDB7093L,
        "papayawhip" to 0xFFFFEFD5L,
        "peachpuff" to 0xFFFFDAB9L,
        "peru" to 0xFFCD853FL,
        "pink" to 0xFFFFC0CBL,
        "plum" to 0xFFDDA0DDL,
        "powderblue" to 0xFFB0E0E6L,
        "purple" to 0xFF800080L,
        "rebeccapurple" to 0xFF663399L,
        "red" to 0xFFFF0000L,
        "rosybrown" to 0xFFBC8F8FL,
        "royalblue" to 0xFF4169E1L,
        "saddlebrown" to 0xFF8B4513L,
        "salmon" to 0xFFFA8072L,
        "sandybrown" to 0xFFF4A460L,
        "seagreen" to 0xFF2E8B57L,
        "seashell" to 0xFFFFF5EEL,
        "sienna" to 0xFFA0522DL,
        "silver" to 0xFFC0C0C0L,
        "skyblue" to 0xFF87CEEBL,
        "slateblue" to 0xFF6A5ACDL,
        "slategray" to 0xFF708090L,
        "slategrey" to 0xFF708090L,
        "snow" to 0xFFFFFAFAL,
        "springgreen" to 0xFF00FF7FL,
        "steelblue" to 0xFF4682B4L,
        "tan" to 0xFFD2B48CL,
        "teal" to 0xFF008080L,
        "thistle" to 0xFFD8BFD8L,
        "tomato" to 0xFFFF6347L,
        "turquoise" to 0xFF40E0D0L,
        "violet" to 0xFFEE82EEL,
        "wheat" to 0xFFF5DEB3L,
        "white" to 0xFFFFFFFFL,
        "whitesmoke" to 0xFFF5F5F5L,
        "yellow" to 0xFFFFFF00L,
        "yellowgreen" to 0xFF9ACD32L
    )
}
