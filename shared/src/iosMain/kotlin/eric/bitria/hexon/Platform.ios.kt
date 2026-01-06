package eric.bitria.hexon

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val baseUrl: String = "http://localhost:8080"
}

actual fun getPlatform(): Platform = IOSPlatform()