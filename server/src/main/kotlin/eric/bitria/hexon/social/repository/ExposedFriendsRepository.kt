package eric.bitria.hexon.social.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Friends
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll


class ExposedFriendsRepository : FriendsRepository {

    override suspend fun getFriendsForUser(userId: String): List<Friend> = dbQuery {
        (Users innerJoin Friends)
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

    // 3. Create a friendship link
    override suspend fun addFriendship(userId1: String, userId2: String): Boolean = dbQuery {
        // We use insertIgnore to safely handle race conditions or duplicates
        // without throwing SQL exceptions.
        val insertStatement = Friends.insertIgnore {
            it[userId] = userId1
            it[friendId] = userId2
        }
        // Returns true if the row was actually added
        insertStatement.insertedCount > 0
    }

    // 4. Delete a friendship link
    override suspend fun removeFriendship(userId1: String, userId2: String): Boolean = dbQuery {
        val deletedCount = Friends.deleteWhere {
            (userId eq userId1) and (friendId eq userId2)
        }
        deletedCount > 0
    }
}