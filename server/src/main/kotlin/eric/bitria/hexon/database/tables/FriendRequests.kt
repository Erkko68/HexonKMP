package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime


object FriendRequests : Table("friend_requests") {
    // Who sent the request
    val requesterId = varchar("requester_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    // Who received the request
    val receiverId = varchar("receiver_id", 36)
        .references(Users.id, onDelete = ReferenceOption.CASCADE)

    val createdAt = datetime("created_at")
        .defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(requesterId, receiverId)
}