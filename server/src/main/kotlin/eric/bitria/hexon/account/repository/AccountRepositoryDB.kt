package eric.bitria.hexon.account.repository

import eric.bitria.hexon.database.DatabaseFactory.dbQuery
import eric.bitria.hexon.database.tables.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

class AccountRepositoryDB : AccountRepository {
    override suspend fun deleteAccountById(id: String) {
        dbQuery {
            Users.deleteWhere { Users.id eq id }
        }
    }
}