package eric.bitria.hexon.users.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.selectAll

class ExposedUserRepository : UserRepository {

    override suspend fun getEmailByUserId(userId: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.id eq userId }
            .map { it[Users.email] }
            .singleOrNull()
    }

    override suspend fun getUserIdByEmail(email: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.id] }
            .singleOrNull()
    }
}