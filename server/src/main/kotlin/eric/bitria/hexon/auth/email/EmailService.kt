package eric.bitria.hexon.auth.email

interface EmailService {
    /**
     * Sends an email to the specified recipient.
     * @param to recipient email address
     * @param subject email subject
     * @param body email body (plain text)
     */
    fun sendEmail(to: String, subject: String, body: String)
}
