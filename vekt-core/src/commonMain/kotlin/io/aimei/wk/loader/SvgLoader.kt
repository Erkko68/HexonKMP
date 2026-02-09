package io.aimei.wk.loader

import io.aimei.wk.model.SvgDocument
import io.aimei.wk.parser.SvgParser

/**
 * Platform-agnostic SVG loader.
 *
 * Provides methods to load SVG content from various sources and parse it
 * into an [SvgDocument]. Platform-specific implementations handle resource
 * and file loading, while string-based loading works on all platforms.
 */
expect object SvgLoader {

    /**
     * Load SVG content from a platform resource path.
     *
     * - **JVM / Android**: classpath resource via `ClassLoader.getResourceAsStream`
     * - **iOS**: `NSBundle.mainBundle` resource
     * - **JS / WasmJs**: not supported, returns `null`
     *
     * @param resourcePath path relative to the resource root (e.g. `"files/icon.svg"`)
     * @return the raw SVG string, or `null` if the resource was not found
     */
    fun loadFromResource(resourcePath: String): String?

    /**
     * Load SVG content from a file system path.
     *
     * - **JVM**: reads via `java.io.File`
     * - **iOS**: reads via `NSFileManager`
     * - **JS / WasmJs**: not supported, returns `null`
     *
     * @param filePath absolute or relative file path
     * @return the raw SVG string, or `null` if the file does not exist or cannot be read
     */
    fun loadFromFile(filePath: String): String?
}

/**
 * Parse an SVG string into an [SvgDocument].
 *
 * This is a convenience extension that delegates to [SvgParser.parse].
 */
fun SvgLoader.parseString(svgContent: String): SvgDocument =
    SvgParser.parse(svgContent)

/**
 * Load and parse an SVG from a platform resource.
 *
 * @return the parsed [SvgDocument], or `null` if the resource was not found
 */
fun SvgLoader.parseResource(resourcePath: String): SvgDocument? =
    loadFromResource(resourcePath)?.let { SvgParser.parse(it) }

/**
 * Load and parse an SVG from a file system path.
 *
 * @return the parsed [SvgDocument], or `null` if the file was not found
 */
fun SvgLoader.parseFile(filePath: String): SvgDocument? =
    loadFromFile(filePath)?.let { SvgParser.parse(it) }
