package eric.bitria.hexon.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import eric.bitria.hexon.data.local.DATA_STORE_FILE_NAME
import eric.bitria.hexon.data.local.DataStoreTokenStorage
import eric.bitria.hexon.data.local.TokenStorage
import eric.bitria.hexon.data.repository.DataStoreUserPreferencesRepository
import eric.bitria.hexon.data.repository.UserPreferencesRepository
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun platformModule(): Module = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
                val path = requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME"
                path.toPath()
            }
        )
    }
    single<TokenStorage> { DataStoreTokenStorage(get()) }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get()) }
}