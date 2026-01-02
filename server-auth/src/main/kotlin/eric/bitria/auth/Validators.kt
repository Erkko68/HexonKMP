package eric.bitria.auth
object Validators {

    // -----------------------------
    // Username: 3-20 chars, letters, numbers, underscores, not empty
    // -----------------------------
    private val USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$".toRegex()
    fun isValidUsername(username: String): Boolean = USERNAME_REGEX.matches(username)

    // -----------------------------
    // Email: standard format, max 254 chars, not empty
    // Local part <= 64, domain part <= 255
    // -----------------------------
    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]{1,32}@[A-Za-z0-9.-]{1,32}\\.[A-Za-z]{2,}$".toRegex()
    fun isValidEmail(email: String): Boolean = email.isNotEmpty() && email.length <= 64 && EMAIL_REGEX.matches(email)

    // -----------------------------
    // Password: 8-20 chars, at least one upper, one lower, one digit, special characters optional
    // Only safe ASCII characters (A-Z, a-z, 0-9, optional _ -)
    // -----------------------------
    private val PASSWORD_REGEX = $$"^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[A-Za-z0-9@#$%^&+=!?._-]{8,32}$".toRegex()
    fun isValidPassword(password: String): Boolean = password.isNotEmpty() && PASSWORD_REGEX.matches(password)

    // -----------------------------
    // Verification code: exactly 6 digits, not empty
    // -----------------------------
    private val VERIFICATION_CODE_REGEX = "^[0-9]{6}$".toRegex()
    fun isValidCode(code: String): Boolean = code.isNotEmpty() && VERIFICATION_CODE_REGEX.matches(code)
}
