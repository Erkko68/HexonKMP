package eric.bitria.hexon.services.social.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Friends
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll


class ExposedFriendsRepository : FriendsRepository {

    override suspend fun getFriendsForUser(userId: String): List<Friend> = dbQuery {
        Users.join(Friends, JoinType.INNER, onColumn = Users.id, otherColumn = Friends.friendId)
            .select(Users.id, Users.username)
            .where { Friends.userId eq userId }
            .map { row ->
                Friend(
                    id = row[Users.id],
                    username = row[Users.username],
                    isOnline = false
                )
            }
    }

    override suspend fun areFriends(userId1: String, userId2: String): Boolean = dbQuery {
        Friends.selectAll()
            .where { (Friends.userId eq userId1) and (Friends.friendId eq userId2) }
            .count() > 0
    }

    override suspend fun addFriendship(userId1: String, userId2: String): Boolean = dbQuery {
        val insertStatement = Friends.insertIgnore {
            it[userId] = userId1
            it[friendId] = userId2
        }
        insertStatement.insertedCount > 0
    }

    override suspend fun removeFriendship(userId1: String, userId2: String): Boolean = dbQuery {
        val deletedCount = Friends.deleteWhere {
            (userId eq userId1) and (friendId eq userId2)
        }
        deletedCount > 0
    }
}
