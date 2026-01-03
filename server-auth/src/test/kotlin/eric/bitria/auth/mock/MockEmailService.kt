package eric.bitria.auth.mock

import eric.bitria.auth.email.EmailService

class MockEmailService(private val inBox: Inbox) : EmailService {
    override fun sendEmail(to: String, subject: String, body: String) {
        inBox.value = body
    }
}

class Inbox(var value: String = "")