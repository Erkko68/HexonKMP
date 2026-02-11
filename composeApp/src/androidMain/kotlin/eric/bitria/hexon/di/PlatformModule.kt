package eric.bitria.hexon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import eric.bitria.hexon.data.DataStoreUserPreferencesRepository
import eric.bitria.hexon.data.UserPreferencesRepository
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                get<Context>().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
            }
        )
    }
    single<TokenStorage> { DataStoreTokenStorage(get()) }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get()) }
}