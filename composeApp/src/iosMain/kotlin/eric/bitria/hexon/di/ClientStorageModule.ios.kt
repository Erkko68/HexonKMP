package eric.bitria.hexon.di

import com.liftric.kvault.KVault
import org.koin.dsl.module

actual val platformStorageModule = module {
    single { KVault() }
}
