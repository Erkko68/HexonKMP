package eric.bitria.hexon

import android.app.Application

class Hexon: Application() {
    override fun onCreate(){
        super.onCreate()
        initKoin()
    }
}