package eric.bitria.hexon.client.di

import org.koin.dsl.module
import platform.UIKit.UIDevice

class IOSPlatform: TargetPlatform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val baseUrl: String = "http://localhost:8080"
}

actual val platformModule = module {
    single<TargetPlatform> { IOSPlatform() }
}
