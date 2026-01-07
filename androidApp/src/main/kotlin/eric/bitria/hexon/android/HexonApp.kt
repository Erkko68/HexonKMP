package eric.bitria.hexon.android

import android.app.Application
import eric.bitria.hexon.initKoinAndroid

class HexonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
