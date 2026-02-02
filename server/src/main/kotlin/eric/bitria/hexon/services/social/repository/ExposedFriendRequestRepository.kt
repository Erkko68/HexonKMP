package eric.bitria.hexon.services.social.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.FriendRequests
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedFriendRequestRepository : FriendRequestRepository {

    override suspend fun hasPendingRequest(requesterId: String, receiverId: String): Boolean = dbQuery {
        FriendRequests.selectAll()
            .where {
                (FriendRequests.requesterId eq requesterId) and
                        (FriendRequests.receiverId eq receiverId)
            }
            .count() > 0
    }

    override suspend fun getIncomingRequests(receiverId: String): List<Friend> = dbQuery {
        Users.join(FriendRequests, JoinType.INNER, onColumn = Users.id, otherColumn = FriendRequests.requesterId)
            .select(Users.id, Users.username)
            .where { FriendRequests.receiverId eq receiverId }
            .map { row ->
                Friend(
                    id = row[Users.id],
                    username = row[Users.username],
                    isOnline = false
                )
            }
    }

    override suspend fun createRequest(requesterId: String, receiverId: String): Boolean = dbQuery {
        val insertStatement = FriendRequests.insertIgnore {
            it[this.requesterId] = requesterId
            it[this.receiverId] = receiverId
        }
        insertStatement.insertedCount > 0
    }

    override suspend fun deleteRequest(requesterId: String, receiverId: String): Boolean = dbQuery {
        val count = FriendRequests.deleteWhere {
            (this.requesterId eq requesterId) and (this.receiverId eq receiverId)
        }
        count > 0
    }
}
