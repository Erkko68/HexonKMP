package eric.bitria.hexon.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table


object Profiles : Table("profiles") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(userId)

    val gamesWon = integer("games_won").default(0)
    val gamesLost = integer("games_lost").default(0)
}