package io.aimei.wk.loader

actual object SvgLoader {

    actual fun loadFromResource(resourcePath: String): String? {
        // Not supported in JS environment — use fetch API or other async mechanisms
        return null
    }

    actual fun loadFromFile(filePath: String): String? {
        // Not supported in browser JS environment
        return null
    }
}
