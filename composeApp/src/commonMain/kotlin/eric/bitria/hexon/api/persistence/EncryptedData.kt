package eric.bitria.hexon.api.persistence

interface EncryptedData {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun putInt(key: String, value: Int)
    fun getInt(key: String): Int?
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean?
    fun remove(key: String)
    fun clear()
}