package eric.bitria.hexon.services.auth.repository

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Sessions
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

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
        }

        User(
            id = newId,
            email = email,
            username = username,
            password = passwordHash,
            isVerified = false
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

    override suspend fun findUserByUsername(username: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.username eq username }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override suspend fun addRefreshToken(userId: String, refreshTokenHash: String, expiresAt: LocalDateTime) = dbQuery {
        Sessions.insert {
            it[this.id] = UuidCreator.getTimeBasedWithRandom().toString()
            it[this.userId] = userId
            it[this.refreshTokenHash] = refreshTokenHash
            it[this.expiresAt] = expiresAt
        }
        Unit
    }

    override suspend fun updateRefreshToken(oldHash: String, newHash: String, newExpiresAt: LocalDateTime): Boolean = dbQuery {
        Sessions.update({ Sessions.refreshTokenHash eq oldHash }) {
            it[this.refreshTokenHash] = newHash
            it[this.expiresAt] = newExpiresAt
        } > 0
    }

    override suspend fun hasRefreshTokenHash(refreshTokenHash: String): Boolean = dbQuery {
        Sessions.selectAll()
            .where { (Sessions.refreshTokenHash eq refreshTokenHash) and (Sessions.expiresAt greater LocalDateTime.now()) }
            .count() > 0
    }

    override suspend fun revokeRefreshToken(refreshTokenHash: String) = dbQuery {
        Sessions.deleteWhere { Sessions.refreshTokenHash eq refreshTokenHash }
        Unit
    }

    override suspend fun revokeAllRefreshTokens(userId: String) = dbQuery {
        Sessions.deleteWhere { Sessions.userId eq userId }
        Unit
    }

    override suspend fun clearExpiredSessions() = dbQuery {
        Sessions.deleteWhere { Sessions.expiresAt less LocalDateTime.now() }
        Unit
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

    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id],
            email = row[Users.email],
            username = row[Users.username],
            password = row[Users.password],
            isVerified = row[Users.isVerified]
        )
    }
}
