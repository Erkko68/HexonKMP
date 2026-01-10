package eric.bitria.hexon.utils

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies

object TokenHasher {

    // Configure BCrypt to pre-hash long inputs (like JWTs) using SHA-512.
    private val longTokenStrategy = LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)

    private val hasher = BCrypt.with(BCrypt.Version.VERSION_2A, longTokenStrategy)
    private val verifier = BCrypt.verifyer(BCrypt.Version.VERSION_2A, longTokenStrategy)

    /**
     * Hashes a long token (JWT) safely.
     */
    fun hash(token: String): String {
        // Cost 10 is good for tokens
        return hasher.hashToString(10, token.toCharArray())
    }

    /**
     * Verifies a long token against a stored hash.
     */
    fun verify(token: String, hash: String): Boolean {
        return verifier.verify(token.toCharArray(), hash).verified
    }
}