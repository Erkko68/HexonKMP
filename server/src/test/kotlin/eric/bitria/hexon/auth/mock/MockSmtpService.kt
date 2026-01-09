package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.email.smtp.SmtpService

class MockSmtpService(private val inBox: Inbox) : SmtpService {
    override fun sendEmail(to: String, subject: String, body: String) {
        inBox.value = body
    }
}

class Inbox(var value: String = "")