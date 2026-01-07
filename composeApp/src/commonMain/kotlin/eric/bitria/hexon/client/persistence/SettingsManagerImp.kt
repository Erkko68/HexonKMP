package eric.bitria.hexon.client.persistence

import com.russhwolf.settings.Settings

class SettingsManagerImpl(private val settings: Settings) : SettingsManager {
    override fun putString(key: String, value: String) = settings.putString(key, value)
    override fun getString(key: String): String? = settings.getStringOrNull(key)
    override fun putInt(key: String, value: Int) = settings.putInt(key, value)
    override fun getInt(key: String, defaultValue: Int): Int = settings.getInt(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = settings.getBoolean(key, defaultValue)
    override fun remove(key: String) = settings.remove(key)
    override fun clear() = settings.clear()
}