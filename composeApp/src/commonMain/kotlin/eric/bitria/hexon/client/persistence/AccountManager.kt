package eric.bitria.hexon.client.persistence

interface AccountManager {
    fun saveEmail(email: String)
    fun getEmail(): String?
    fun clear()
}

class AccountManagerImpl(private val settingsManager: SettingsManager) : AccountManager {
    companion object {
        private const val KEY_EMAIL = "user_email"
    }

    override fun saveEmail(email: String) {
        settingsManager.putString(KEY_EMAIL, email)
    }

    override fun getEmail(): String? {
        return settingsManager.getString(KEY_EMAIL)
    }

    override fun clear() {
        settingsManager.remove(KEY_EMAIL)
    }
}
