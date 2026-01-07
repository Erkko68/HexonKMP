package eric.bitria.hexon.persistence

import com.liftric.kvault.KVault

class EncryptedDataImpl(private val kVault: KVault) : EncryptedData {
    override fun putString(key: String, value: String) {
        kVault.set(key, value)
    }

    override fun getString(key: String): String? {
        return kVault.string(key)
    }

    override fun putInt(key: String, value: Int) {
        kVault.set(key, value)
    }

    override fun getInt(key: String): Int? {
        return kVault.int(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        kVault.set(key, value)
    }

    override fun getBoolean(key: String): Boolean? {
        return kVault.bool(key)
    }

    override fun remove(key: String) {
        kVault.deleteObject(key)
    }

    override fun clear() {
        kVault.clear()
    }
}
