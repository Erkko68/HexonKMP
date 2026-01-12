package eric.bitria.hexon.social.repository

data class Friend(
    val id: String,
    val username: String,
    val isOnline: Boolean = false
)
