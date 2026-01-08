package eric.bitria.hexon.client.di

import eric.bitria.hexon.BuildKonfig
import org.koin.dsl.module
import platform.UIKit.UIDevice

class IOSPlatform: TargetPlatform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val baseUrl: String = BuildKonfig.BASE_URL
}

actual val platformModule = module {
    single<TargetPlatform> { IOSPlatform() }
}
