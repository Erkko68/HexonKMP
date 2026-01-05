package eric.bitria.hexon.android

import android.app.Application
import eric.bitria.hexon.initKoin

class HexonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
