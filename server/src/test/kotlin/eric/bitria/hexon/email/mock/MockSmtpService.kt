package eric.bitria.hexon.email.mock

import eric.bitria.hexon.services.email.smtp.SmtpService

class MockSmtpService : SmtpService {
    private val sentEmails = mutableListOf<SentEmail>()

    data class SentEmail(val to: String, val subject: String, val body: String)

    override fun sendEmail(to: String, subject: String, body: String) {
        sentEmails.add(SentEmail(to, subject, body))
    }

    fun getSentEmails(): List<SentEmail> = sentEmails

    fun getLastEmailTo(email: String): SentEmail? = sentEmails.lastOrNull { it.to == email }

    fun clear() {
        sentEmails.clear()
    }
}
