package eric.bitria.hexon.services.social.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.FriendRequests
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

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
        (Users innerJoin FriendRequests)
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