package eric.bitria.hexon.viewmodel.data

data class Friend(
    val id: Int,
    val username: String,
    val isOnline: Boolean,
    val avatarUrl: String? = null
)
