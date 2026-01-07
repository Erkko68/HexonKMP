package eric.bitria.hexon.client.di

import com.liftric.kvault.KVault
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformStorageModule = module {
    single { KVault(androidContext(), "hexon_secure_pref") }
}
