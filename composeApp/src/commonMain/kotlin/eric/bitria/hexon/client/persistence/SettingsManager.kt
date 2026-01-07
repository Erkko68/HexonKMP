package eric.bitria.hexon.client.persistence

interface SettingsManager {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun putInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun remove(key: String)
    fun clear()
}