package com.carslab.crm.modules.email.infrastructure.services

import com.carslab.crm.domain.model.EmailAttachment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.email.domain.ports.EmailSender
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.domain.model.EmailSettings
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import jakarta.mail.AuthenticationFailedException
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.core.io.ByteArrayResource
import java.util.*
import jakarta.mail.internet.MimeMessage

@Service
class SmtpEmailSender(
    private val companySettingsDomainService: CompanySettingsDomainService,
    private val securityContext: SecurityContext,
) : EmailSender {

    private val logger = LoggerFactory.getLogger(SmtpEmailSender::class.java)

    override fun sendEmail(
        recipientEmail: String,
        subject: String,
        htmlContent: String,
        senderName: String?,
        senderEmail: String?,
        attachment: EmailAttachment?
    ): Boolean {
        return try {
            val companyId = securityContext.getCurrentCompanyId()
            val companySettings = companySettingsDomainService.getCompanySettings(companyId)
                ?: throw IllegalStateException("Company settings not found")
            
            val emailSettings = companySettings.emailSettings
            if (!emailSettings.hasValidSmtpConfig()) {
                throw IllegalStateException("SMTP configuration is not valid")
            }

            if (attachment != null) {
                logger.info("Protocol document found: ${attachment.size} bytes")
            } else {
                logger.warn("Protocol document not found")
            }
            logger.info("=====================================")

            val mailSender = createFixedMailSender(emailSettings)
            val message: MimeMessage = mailSender.createMimeMessage()

            // Użyj multipart=true dla załączników
            val hasAttachments = attachment != null
            val helper = MimeMessageHelper(message, hasAttachments, "UTF-8")

            helper.setTo(recipientEmail)
            helper.setSubject(subject)
            helper.setText(htmlContent, true)

            // Ustaw nadawcę
            val fromEmail = senderEmail ?: emailSettings.senderEmail ?: emailSettings.smtpUsername!!
            val fromName = senderName ?: emailSettings.senderName ?: companySettings.basicInfo.companyName
            helper.setFrom(fromEmail, fromName)

            // Dodaj protokół jako załącznik jeśli istnieje
            if (attachment != null) {
                try {
                    helper.addAttachment(
                        attachment.filename,
                        ByteArrayResource(attachment.data!!),
                        "application/pdf"
                    )
                    logger.info("Added protocol attachment: ${attachment.filename} (${attachment.size} bytes)")
                } catch (e: Exception) {
                    logger.error("Failed to add protocol attachment", e)
                    // Kontynuuj wysyłanie bez załącznika
                }
            }

            logger.info("Attempting to send email to: $recipientEmail")
            mailSender.send(message)
            logger.info("Successfully sent email to: $recipientEmail")

            true

        } catch (e: AuthenticationFailedException) {
            logger.error("=== AUTHENTICATION FAILED ===")
            logger.error("Error: ${e.message}")
            logger.error("This usually means:")
            logger.error("1. Wrong username or password")
            logger.error("2. Not using App Password (for Gmail)")
            logger.error("3. 2FA not enabled (for Gmail)")
            logger.error("4. Password contains wrong characters")
            logger.error("===============================")
            false
        } catch (e: Exception) {
            logger.error("Failed to send email to: $recipientEmail", e)
            false
        }
    }


    private fun createFixedMailSender(emailSettings: EmailSettings): JavaMailSenderImpl {
        val mailSender = JavaMailSenderImpl()

        // Podstawowe ustawienia
        mailSender.host = emailSettings.smtpHost!!
        mailSender.port = emailSettings.smtpPort!!
        mailSender.username = emailSettings.smtpUsername!!

        // WAŻNE: Usuń wszystkie spacje z hasła App Password
        val cleanPassword = emailSettings.smtpPassword!!.replace(" ", "")
        mailSender.password = cleanPassword

        logger.info("Using cleaned password length: ${cleanPassword.length}")

        val props: Properties = mailSender.javaMailProperties

        // TYLKO dla Gmail - specjalna konfiguracja
        if (emailSettings.smtpHost?.contains("gmail.com") == true) {
            logger.info("Configuring for Gmail SMTP")

            props.clear() // Wyczyść wszystkie właściwości

            // Podstawowe
            props["mail.transport.protocol"] = "smtp"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.host"] = emailSettings.smtpHost
            props["mail.smtp.port"] = emailSettings.smtpPort.toString()

            if (emailSettings.smtpPort == 587) {
                // TLS na porcie 587
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.starttls.required"] = "true"
                props["mail.smtp.ssl.enable"] = "false"
                props["mail.smtp.ssl.protocols"] = "TLSv1.2"
            } else if (emailSettings.smtpPort == 465) {
                // SSL na porcie 465
                props["mail.smtp.ssl.enable"] = "true"
                props["mail.smtp.starttls.enable"] = "false"
                props["mail.smtp.socketFactory.port"] = "465"
                props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                props["mail.smtp.socketFactory.fallback"] = "false"
            }

            // Timeouty
            props["mail.smtp.connectiontimeout"] = "30000"
            props["mail.smtp.timeout"] = "30000"
            props["mail.smtp.writetimeout"] = "30000"

            // Bezpieczeństwo
            props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
            props["mail.smtp.ssl.checkserveridentity"] = "true"

            // Debug - WŁĄCZ dla debugowania
            props["mail.debug"] = "false" // Zmień na "true" do debugowania
        }

        logger.info("Final SMTP properties: $props")

        return mailSender
    }
}