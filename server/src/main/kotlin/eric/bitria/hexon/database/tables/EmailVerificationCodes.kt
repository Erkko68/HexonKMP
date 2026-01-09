package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object EmailVerificationCodes : Table("email_verification_codes") {
    val email = varchar("email", 64)
        .references(Users.email, onDelete = ReferenceOption.CASCADE)

    val codeHash = varchar("code_hash", 255)

    // We store the type so the app knows what this code allows the user to do.
    val type = enumerationByName("type", 50, EmailVerificationType::class)

    val expiresAt = datetime("expires_at")
    val attempts = integer("attempts").default(0)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(email)
}

enum class EmailVerificationType {
    EMAIL_CONFIRMATION,
    PASSWORD_RESET,
    ACCOUNT_DELETION
}