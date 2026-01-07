package eric.bitria.hexon.client.di

import com.liftric.kvault.KVault
import org.koin.dsl.module

actual val platformStorageModule = module {
    single { KVault() }
}
