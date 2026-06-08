package eric.bitria.hexonkmp.auth

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Anonymous device identity: maps a secret bearer [token] -> a server-issued
// playerId. The client stores the token locally and presents it on every request;
// the server resolves the playerId from it, so a client can never act as a player
// whose token it doesn't hold (knowing the public playerId alone is useless).
//
// Opaque + in-memory on purpose: the server keeps all session state in memory too,
// so there's nothing to gain from stateless (JWT) tokens at this scale. issue/resolve
// are the seam to swap in JWTs later if the server ever scales out.
class TokenRegistry {
    private val playerIdByToken = ConcurrentHashMap<String, String>()
    private val random = SecureRandom()

    // Reuse the identity behind [token] if it's valid (reconnection / new session on
    // the same device), otherwise mint a fresh playerId + token. Returns (playerId, token).
    fun registerOrReuse(token: String?): Pair<String, String> {
        if (token != null) {
            playerIdByToken[token]?.let { return it to token }
        }
        val playerId = UUID.randomUUID().toString()
        val fresh = newToken()
        playerIdByToken[fresh] = playerId
        return playerId to fresh
    }

    // The playerId a token authenticates, or null if the token is unknown.
    fun resolve(token: String): String? = playerIdByToken[token]

    private fun newToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}
