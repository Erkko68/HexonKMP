package eric.bitria.hexon.email.smtp

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class SmtpServiceImp(
    private val smtpConfig: SmtpConfig
) : SmtpService {
    private val session: Session

    init {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.host", smtpConfig.smtpHost)
            put("mail.smtp.port", smtpConfig.smtpPort)
            
            if (smtpConfig.smtpPort == "465") {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
        }

        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtpConfig.smtpUser, smtpConfig.smtpPassword)
        })
    }

    override fun sendEmail(to: String, subject: String, body: String) {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(smtpConfig.smtpUser))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            this.subject = subject
            setText(body)
        }
        Transport.send(message)
    }
}
