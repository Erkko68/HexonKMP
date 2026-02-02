package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime


object Friends : Table("friends") {
    val userId = varchar("user_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    val friendId = varchar("friend_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    val createdAt = datetime("created_at")
        .defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, friendId)
}