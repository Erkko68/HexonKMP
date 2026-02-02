package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime


object Sessions : Table("sessions") {
    val id = varchar("id", 36)
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val refreshTokenHash = varchar("refresh_hash", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(id)
}
