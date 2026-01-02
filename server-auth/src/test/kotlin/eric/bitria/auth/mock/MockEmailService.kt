package eric.bitria.auth.mock

import eric.bitria.auth.email.EmailService

class MockEmailService : EmailService {
    override fun sendEmail(to: String, subject: String, body: String) {
    }
}