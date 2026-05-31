package eric.bitria.hexonkmp

import android.app.Application
import eric.bitria.hexonkmp.data.storage.AppContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.app = this
    }
}
