package eric.bitria.hexon

import android.content.Context
import eric.bitria.hexon.di.initKoin
import org.koin.android.ext.koin.androidContext

fun initKoinAndroid(context: Context) {
    initKoin(persistRefreshToken = true) {
        androidContext(context)
    }
}
