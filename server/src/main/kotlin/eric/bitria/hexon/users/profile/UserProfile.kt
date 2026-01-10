package eric.bitria.hexon.users.profile

data class UserProfile(
    val userId: String,
    val email: String,
    val username: String,
    val gamesWon: Int,
    val gamesLost: Int
)
