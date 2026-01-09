package eric.bitria.hexon.email.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.EmailVerificationCodes
import eric.bitria.hexon.database.tables.EmailVerificationType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class ExposedEmailVerificationRepository(
    private val database: Database
) : EmailVerificationRepository {

    override suspend fun saveVerificationCode(
        email: String,
        codeHash: String,
        type: EmailVerificationType,
        expiresAt: Instant
    ) = dbQuery {
        EmailVerificationCodes.upsert {
            it[this.email] = email
            it[this.codeHash] = codeHash
            it[this.type] = type
            it[this.expiresAt] = expiresAt.toLocalDateTime()
            it[this.createdAt] = java.time.LocalDateTime.now(ZoneOffset.UTC)
            it[this.attempts] = 0
        }
        Unit
    }

    override suspend fun getVerificationCodeHash(
        email: String,
        requiredType: EmailVerificationType
    ): String? = dbQuery {
        val now = java.time.LocalDateTime.now(ZoneOffset.UTC)

        // Select the code only if:
        // 1. Email matches
        // 2. The Type matches (Security check)
        // 3. It hasn't expired
        EmailVerificationCodes
            .selectAll()
            .where {
                (EmailVerificationCodes.email eq email) and
                        (EmailVerificationCodes.type eq requiredType) and
                        (EmailVerificationCodes.expiresAt greater now)
            }
            .map { it[EmailVerificationCodes.codeHash] }
            .singleOrNull()
    }

    override suspend fun incrementAttempts(email: String) = dbQuery {
        with(SqlExpressionBuilder) {
            EmailVerificationCodes.update({ EmailVerificationCodes.email eq email }) {
                it.update(attempts, attempts + 1)
            }
        }
        Unit
    }

    override suspend fun deleteVerificationCode(email: String) = dbQuery {
        EmailVerificationCodes.deleteWhere {
            this.email eq email
        }
        Unit
    }

    override suspend fun deleteExpiredCodes() = dbQuery {
        val now = java.time.LocalDateTime.now(ZoneOffset.UTC)
        EmailVerificationCodes.deleteWhere {
            expiresAt less now
        }
        Unit
    }

    // --- Helpers ---

    private fun Instant.toLocalDateTime(): java.time.LocalDateTime {
        return this.toJavaInstant()
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime()
    }
}