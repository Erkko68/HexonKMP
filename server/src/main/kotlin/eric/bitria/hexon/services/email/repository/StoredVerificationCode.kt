package eric.bitria.hexon.services.email.repository

import eric.bitria.hexon.dtos.auth.EmailVerificationType
import kotlin.time.Instant

data class StoredVerificationCode(
    val codeHash: String,
    val type: EmailVerificationType,
    val expiresAt: Instant
)