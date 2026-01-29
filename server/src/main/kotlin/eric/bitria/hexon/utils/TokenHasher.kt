package eric.bitria.hexon.utils

import java.security.MessageDigest
import java.util.Base64

object TokenHasher {

    /**
     * Hashes a token deterministically using SHA-256.
     * Required for database lookups by hash.
     */
    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * Verifies a token against a stored hash.
     */
    fun verify(token: String, hash: String): Boolean {
        return hash(token) == hash
    }
}
