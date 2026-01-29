package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Sessions : Table("sessions") {
    val id = varchar("id", 36)
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val refreshTokenHash = varchar("refresh_hash", 255).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
