package com.carslab.crm.modules.communication.infrastructure.mail

import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

data class OutgoingReply(
    val to: String,
    val subject: String,
    val bodyText: String,
    val bodyHtml: String,
    /** Message-ID of the message being replied to (normalized, no brackets). */
    val inReplyTo: String?,
    /** Full References chain, oldest first, already capped by the caller. */
    val references: List<String>
)

/**
 * SMTP sender that owns the RFC 5322 threading headers: it generates the
 * Message-ID itself (persisted by the caller BEFORE sending — transactional
 * outbox) and sets In-Reply-To/References, so the client's reply is guaranteed
 * to link back deterministically.
 */
@Service
class ThreadAwareSmtpSender(
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(ThreadAwareSmtpSender::class.java)

    fun newMessageId(account: MailAccountEntity): String {
        val domain = account.emailAddress.substringAfterLast('@', "carslab.app")
        return "${UUID.randomUUID()}@$domain"
    }

    fun send(account: MailAccountEntity, messageId: String, reply: OutgoingReply) {
        val host = account.smtpHost
            ?: throw IllegalStateException("Mail account ${account.id} has no SMTP host configured")
        val port = account.smtpPort ?: 587
        val password = account.encryptedPassword?.let { encryptionService.decrypt(it) }
            ?: throw IllegalStateException("Mail account ${account.id} has no credentials")

        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            if (port == 465) {
                put("mail.smtp.ssl.enable", "true")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "20000")
        }
        val session = Session.getInstance(props)

        val message = object : MimeMessage(session) {
            override fun updateMessageID() {
                setHeader("Message-ID", "<$messageId>")
            }
        }
        message.setFrom(InternetAddress(account.emailAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reply.to))
        message.setSubject(reply.subject, "UTF-8")
        reply.inReplyTo?.let { message.setHeader("In-Reply-To", "<$it>") }
        if (reply.references.isNotEmpty()) {
            message.setHeader("References", reply.references.joinToString(" ") { "<$it>" })
        }
        message.setContent(reply.bodyHtml, "text/html; charset=UTF-8")

        Transport.send(message, account.emailAddress, password)
        logger.info("Sent reply <$messageId> from account ${account.id} to ${reply.to}")
    }
}
