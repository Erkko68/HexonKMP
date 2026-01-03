package eric.bitria.auth.utils

/**
 * Collection of input validation helpers used by the authentication layer.
 *
 * All validators are:
 * - Stateless
 * - Pure functions
 * - Safe to call from UI, ViewModels, or backend code
 */
object Validators {

    /**
     * Username validation rules:
     * - Length: 3–20 characters
     * - Allowed characters: letters (A–Z, a–z), digits (0–9), underscore (_)
     * - No spaces or special characters
     */
    private val USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$".toRegex()

    /**
     * Checks whether a username is valid according to [USERNAME_REGEX].
     */
    fun isValidUsername(username: String): Boolean =
        USERNAME_REGEX.matches(username)

    /**
     * Email validation rules (simplified, not RFC-complete):
     * - Non-empty
     * - Maximum length: 64 characters (application-level constraint)
     * - Local part: letters, digits, and common symbols (._%+-)
     * - Domain: letters, digits, dots, and hyphens
     * - Top-level domain: at least 2 letters
     *
     */
    private val EMAIL_REGEX =
        "^[A-Za-z0-9._%+-]{1,32}@[A-Za-z0-9.-]{1,32}\\.[A-Za-z]{2,}$".toRegex()

    /**
     * Checks whether an email address is valid and within length limits.
     */
    fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() &&
                email.length <= 64 &&
                EMAIL_REGEX.matches(email)

    /**
     * Password validation rules:
     * - Length: 8–32 characters
     * - Must contain at least:
     *   - One uppercase letter
     *   - One lowercase letter
     *   - One digit
     * - Allowed characters:
     *   A–Z, a–z, 0–9, and common safe symbols (@#$%^&+=!?._-)
     *
     * Special characters are optional but allowed.
     */
    private val PASSWORD_REGEX =
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[A-Za-z0-9@#$%^&+=!?._-]{8,32}$"
            .toRegex()

    /**
     * Checks whether a password meets strength and format requirements.
     */
    fun isValidPassword(password: String): Boolean =
        password.isNotEmpty() &&
                PASSWORD_REGEX.matches(password)

    /**
     * Verification code validation rules:
     * - Exactly 6 digits
     * - No letters or symbols
     * - Typically used for email or SMS verification
     */
    private val VERIFICATION_CODE_REGEX = "^[0-9]{6}$".toRegex()

    /**
     * Checks whether a verification code is valid.
     */
    fun isValidCode(code: String): Boolean =
        code.isNotEmpty() &&
                VERIFICATION_CODE_REGEX.matches(code)
}
