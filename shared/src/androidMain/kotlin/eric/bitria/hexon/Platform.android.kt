package eric.bitria.hexon

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val baseUrl: String = "http://10.0.2.2:8080"
}

actual fun getPlatform(): Platform = AndroidPlatform()