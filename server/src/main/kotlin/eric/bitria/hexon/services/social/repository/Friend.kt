package eric.bitria.hexon.services.social.repository

data class Friend(
    val id: String,
    val username: String,
    val isOnline: Boolean = false
)
