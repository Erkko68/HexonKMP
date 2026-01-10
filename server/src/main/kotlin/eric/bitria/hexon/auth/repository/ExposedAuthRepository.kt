package eric.bitria.hexon.auth.repository

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedAuthRepository : AuthRepository {

    override suspend fun isEmailRegistered(email: String): Boolean = dbQuery {
        Users.selectAll().where { Users.email eq email }.count() > 0
    }

    override suspend fun isUsernameTaken(username: String): Boolean = dbQuery {
        Users.selectAll().where { Users.username eq username }.count() > 0
    }

    override suspend fun createUser(
        email: String,
        username: String,
        passwordHash: String
    ): User = dbQuery {
        val newId = UuidCreator.getTimeBasedWithRandom().toString()

        Users.insert {
            it[this.id] = newId
            it[this.email] = email
            it[this.username] = username
            it[this.password] = passwordHash
            it[this.isVerified] = false
            it[this.refreshTokenHash] = null
        }

        // Return the data class directly
        User(
            id = newId,
            email = email,
            username = username,
            password = passwordHash,
            isVerified = false,
            refreshTokenHash = null
        )
    }

    override suspend fun findUserByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override suspend fun findUserById(userId: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq userId }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override suspend fun updateRefreshToken(userId: String, refreshTokenHash: String?) = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[this.refreshTokenHash] = refreshTokenHash
        }
        Unit
    }

    override suspend fun getRefreshTokenHash(userId: String): String? = dbQuery {
        Users.select(Users.refreshTokenHash)
            .where { Users.id eq userId }
            .map { it[Users.refreshTokenHash] }
            .singleOrNull()
    }

    override suspend fun verifyUser(userId: String): Unit = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[isVerified] = true
        }
    }

    override suspend fun updatePassword(userId: String, newPasswordHash: String) = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[password] = newPasswordHash
        }
        Unit
    }

    override suspend fun deleteUser(userId: String) = dbQuery {
        Users.deleteWhere { Users.id eq userId }
        Unit
    }

    // --- Helper Mapping ---
    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id],
            email = row[Users.email],
            username = row[Users.username],
            password = row[Users.password],
            isVerified = row[Users.isVerified],
            refreshTokenHash = row[Users.refreshTokenHash]
        )
    }
}