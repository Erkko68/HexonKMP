package eric.bitria.hexon.utils

import java.security.MessageDigest
import java.util.Base64

fun hashToken(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(token.toByteArray())
    return Base64.getEncoder().encodeToString(hashBytes)
}