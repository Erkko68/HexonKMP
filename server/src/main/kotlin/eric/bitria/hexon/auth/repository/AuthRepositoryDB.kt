package eric.bitria.hexon.auth.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class AuthRepositoryDB : AuthRepository {
    override suspend fun usernameExists(username: String): Boolean = dbQuery {
        Users
            .selectAll()
            .where { Users.username eq username }
            .count() > 0
    }

    override suspend fun emailExists(email: String): Boolean = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .count() > 0
    }

    override suspend fun isAccountVerified(email: String): Boolean = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.isVerified] }
            .singleOrNull() ?: false
    }

    override suspend fun getVerificationCodeByEmail(email: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.verificationCode] }
            .singleOrNull()
    }

    override suspend fun markAccountAsVerified(email: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[isVerified] = true
                it[verificationCode] = null
            }
        }
    }

    override suspend fun saveOrUpdateUnverifiedUser(
        email: String,
        username: String,
        password: String,
        verificationCode: String
    ) {
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        dbQuery {
            val existingUser = Users
                .selectAll()
                .where { Users.email eq email }
                .singleOrNull()

            if (existingUser == null) {
                Users.insert {
                    it[id] = UuidCreator.getTimeBasedWithRandom().toString()
                    it[Users.email] = email
                    it[Users.username] = username
                    it[Users.password] = hashedPassword
                    it[Users.verificationCode] = verificationCode
                    it[isVerified] = false
                }
            } else if (!existingUser[Users.isVerified]) {
                Users.update({ Users.email eq email }) {
                    it[Users.username] = username
                    it[Users.password] = hashedPassword
                    it[Users.verificationCode] = verificationCode
                }
            }
        }
    }

    override suspend fun getUserIdByEmail(email: String): String = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.id] }
            .singleOrNull() ?: ""
    }

    override suspend fun getEmailByUsername(username: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.username eq username }
            .map { it[Users.email] }
            .singleOrNull()
    }

    override suspend fun updateVerificationCode(email: String, verificationCode: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[Users.verificationCode] = verificationCode
            }
        }
    }

    override suspend fun getPasswordByEmail(email: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.password] }
            .singleOrNull()
    }

    override suspend fun updatePassword(email: String, passwordHash: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[password] = passwordHash
            }
        }
    }

    override suspend fun updateResetCode(email: String, resetCode: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[Users.resetCode] = resetCode
            }
        }
    }

    override suspend fun getResetCodeByEmail(email: String): String? = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email }
            .map { it[Users.resetCode] }
            .singleOrNull()
    }

    override suspend fun clearResetCode(email: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[resetCode] = null
            }
        }
    }
}
