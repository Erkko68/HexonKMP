package eric.bitria.hexon.users.profile

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Profiles
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedProfileRepository : ProfileRepository {

    override suspend fun createProfile(userId: String): Unit = dbQuery {
        // Using insertIgnore prevents crashes if we accidentally call this twice
        Profiles.insertIgnore {
            it[Profiles.userId] = userId
            it[gamesWon] = 0
            it[gamesLost] = 0
        }
    }

    override suspend fun getUserProfile(userId: String): UserProfile? = dbQuery {
        Users.join(Profiles, JoinType.LEFT, additionalConstraint = { Profiles.userId eq Users.id })
            .selectAll()
            .where { Users.id eq userId }
            .map { row ->
                UserProfile(
                    userId = row[Users.id],
                    email = row[Users.email],
                    username = row[Users.username],
                    gamesWon = row.getOrNull(Profiles.gamesWon) ?: 0,
                    gamesLost = row.getOrNull(Profiles.gamesLost) ?: 0
                )
            }
            .singleOrNull()
    }

    override suspend fun updateStats(userId: String, isWin: Boolean): Unit = dbQuery {
        Profiles.update({ Profiles.userId eq userId }) {
            if (isWin) {
                // Atomic increment: games_won = games_won + 1
                with(SqlExpressionBuilder) {
                    it.update(gamesWon, gamesWon + 1)
                }
            } else {
                with(SqlExpressionBuilder) {
                    it.update(gamesLost, gamesLost + 1)
                }
            }
        }
    }
}