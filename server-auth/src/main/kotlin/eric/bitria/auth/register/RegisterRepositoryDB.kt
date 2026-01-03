package eric.bitria.auth.register

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.auth.database.DatabaseFactory.dbQuery
import eric.bitria.auth.database.Users
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class RegisterRepositoryDB : RegisterRepository {
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

    override suspend fun saveUser(
        email: String,
        username: String,
        password: String,
        verificationCode: String
    ) {
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        dbQuery {
            Users.insert {
                it[id] = UuidCreator.getTimeBasedWithRandom().toString()
                it[Users.email] = email
                it[Users.username] = username
                it[Users.password] = hashedPassword
                it[Users.verificationCode] = verificationCode
                it[isVerified] = false
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

    override suspend fun verifyEmail(
        email: String,
        code: String
    ): VerifyEmailResult = dbQuery {
        val user = Users
            .selectAll()
            .where { Users.email eq email }
            .singleOrNull() ?: return@dbQuery VerifyEmailResult.INVALID_EMAIL

        if (user[Users.isVerified]) {
            return@dbQuery VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED
        }

        if (user[Users.verificationCode] == code) {
            Users.update({ Users.email eq email }) {
                it[isVerified] = true
                it[verificationCode] = null
            }
            VerifyEmailResult.SUCCESS
        } else {
            VerifyEmailResult.INVALID_VERIFICATION_CODE
        }
    }

    override suspend fun updateVerificationCode(email: String, verificationCode: String) {
        dbQuery {
            Users.update({ Users.email eq email }) {
                it[Users.verificationCode] = verificationCode
            }
        }
    }
}
