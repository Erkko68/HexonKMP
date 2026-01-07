package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 64).uniqueIndex()
    val username = varchar("username", 20).uniqueIndex()
    val password = varchar("password", 255)
    val verificationCode = varchar("verification_code", 6).nullable()
    val isVerified = bool("is_verified").default(false)
    val resetCode = varchar("reset_code", 6).nullable()

    override val primaryKey = PrimaryKey(id)
}
