package io.aimei.wk.loader

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

actual object SvgLoader {

    @OptIn(ExperimentalForeignApi::class)
    actual fun loadFromResource(resourcePath: String): String? {
        // Split "dir/file.svg" into directory and filename parts
        val lastSlash = resourcePath.lastIndexOf('/')
        val (subdir, fileName) = if (lastSlash >= 0) {
            resourcePath.substring(0, lastSlash) to resourcePath.substring(lastSlash + 1)
        } else {
            null to resourcePath
        }
        val dotIndex = fileName.lastIndexOf('.')
        val (name, ext) = if (dotIndex >= 0) {
            fileName.substring(0, dotIndex) to fileName.substring(dotIndex + 1)
        } else {
            fileName to null
        }
        val path = NSBundle.mainBundle.pathForResource(name, ext, subdir) ?: return null
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun loadFromFile(filePath: String): String? {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(filePath)) return null
        return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
    }
}
