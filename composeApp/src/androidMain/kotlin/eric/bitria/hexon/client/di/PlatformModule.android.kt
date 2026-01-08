package eric.bitria.hexon.client.di

import eric.bitria.hexon.BuildKonfig
import android.os.Build
import org.koin.dsl.module

class AndroidPlatform : TargetPlatform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val baseUrl: String = BuildKonfig.BASE_URL
}

actual val platformModule = module {
    single<TargetPlatform> { AndroidPlatform() }
}
