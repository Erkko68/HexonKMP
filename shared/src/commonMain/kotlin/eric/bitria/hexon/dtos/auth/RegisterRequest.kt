package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

/**
 * Represents a request to register a new user.
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

/**
 * Represents the result of a registration request.
 */
@Serializable
data class RegisterResponse(
    val result: RegisterResult,
    val message: String,
    val email: String
)

/**
 * Represents a request to verify a user's email address.
 */
@Serializable
data class VerifyEmailRequest(
    val email: String,
    val verificationCode: String
)

/**
 * Represents the result of a verification request.
 */
@Serializable
data class VerifyEmailResponse(
    val result: VerifyEmailResult,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
enum class RegisterResult {
    VERIFICATION_SENT,
    SUCCESS,
    USERNAME_EXISTS,
    EMAIL_EXISTS,
    INVALID_USERNAME,
    INVALID_EMAIL,
    INVALID_PASSWORD,
    UNKNOWN_ERROR
}

@Serializable
enum class VerifyEmailResult {
    SUCCESS,
    INVALID_EMAIL,
    INVALID_VERIFICATION_CODE,
    UNKNOWN_ERROR
}