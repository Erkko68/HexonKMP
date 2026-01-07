package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Friends : Table("friends") {
    val userId = varchar("user_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    val friendId = varchar("friend_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    val createdAt = datetime("created_at")
        .default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(userId, friendId)
}
