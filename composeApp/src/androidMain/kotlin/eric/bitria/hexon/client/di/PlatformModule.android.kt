package eric.bitria.hexon.client.di

import android.os.Build
import org.koin.dsl.module

class AndroidPlatform : TargetPlatform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val baseUrl: String = "http://10.0.2.2:8080"
}

actual val platformModule = module {
    single<TargetPlatform> { AndroidPlatform() }
}
