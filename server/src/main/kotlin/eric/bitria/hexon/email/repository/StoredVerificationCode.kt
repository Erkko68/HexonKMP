package eric.bitria.hexon.email.repository

import eric.bitria.hexon.database.tables.EmailVerificationType
import kotlin.time.Instant

data class StoredVerificationCode(
    val codeHash: String,
    val type: EmailVerificationType,
    val expiresAt: Instant
)