package io.aimei.wk.loader

import java.io.File

actual object SvgLoader {

    actual fun loadFromResource(resourcePath: String): String? {
        return SvgLoader::class.java.classLoader
            ?.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.readText()
    }

    actual fun loadFromFile(filePath: String): String? {
        val file = File(filePath)
        return if (file.exists()) file.readText() else null
    }
}
