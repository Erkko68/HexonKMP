package eric.bitria.hexon.services.auth.repository

data class User(
    val id: String,
    val email: String,
    val username: String,
    val password: String,
    val isVerified: Boolean,
    val refreshTokenHash: String?
)
