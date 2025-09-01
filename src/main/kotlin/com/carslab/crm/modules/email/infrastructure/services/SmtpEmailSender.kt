package com.carslab.crm.modules.email.infrastructure.services

import com.carslab.crm.modules.email.domain.ports.EmailSender
import com.carslab.crm.modules.email.domain.ports.EmailConfigurationRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.modules.email.domain.ports.EmailAttachment
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
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
    private val encryptionService: EncryptionService,
    private val securityContext: SecurityContext,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
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
            val emailConfig = companyDetailsFetchService.getCompanySettings(companyId)
                .mailConfiguration

            if (!emailConfig.enabled) {
                throw IllegalStateException("Email configuration is disabled for company: $companyId")
            }

            val decryptedPassword = encryptionService.decrypt(emailConfig.emailPassword ?: "")

            val mailSender = createMailSender(
                emailConfig.email ?: "",
                decryptedPassword,
                emailConfig.smtpServer ?: "",
                emailConfig.smtpPort ?: 0,
                emailConfig.useSsl
            )

            val message: MimeMessage = mailSender.createMimeMessage()
            val hasAttachments = attachment != null
            val helper = MimeMessageHelper(message, hasAttachments, "UTF-8")

            helper.setTo(recipientEmail)
            helper.setSubject(subject)
            helper.setText(htmlContent, true)

            val fromEmail = senderEmail ?: emailConfig.email ?: ""
            val fromName = senderName ?: emailConfig.fromName ?: ""
            helper.setFrom(fromEmail, fromName)

            if (attachment != null) {
                try {
                    helper.addAttachment(
                        attachment.filename,
                        ByteArrayResource(attachment.content),
                        attachment.contentType
                    )
                    logger.debug("Added attachment: ${attachment.filename})")
                } catch (e: Exception) {
                    logger.error("Failed to add attachment", e)
                }
            }

            logger.info("Sending email to: $recipientEmail")
            mailSender.send(message)
            logger.info("Successfully sent email to: $recipientEmail")

            true

        } catch (e: AuthenticationFailedException) {
            logger.error("Authentication failed when sending email", e)
            false
        } catch (e: Exception) {
            logger.error("Failed to send email to: $recipientEmail", e)
            false
        }
    }

    private fun createMailSender(
        username: String,
        password: String,
        smtpHost: String,
        smtpPort: Int,
        useSSL: Boolean
    ): JavaMailSenderImpl {
        val mailSender = JavaMailSenderImpl()

        mailSender.host = smtpHost
        mailSender.port = smtpPort
        mailSender.username = username
        mailSender.password = password.replace(" ", "")

        val props: Properties = mailSender.javaMailProperties
        props.clear()

        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.host"] = smtpHost
        props["mail.smtp.port"] = smtpPort.toString()

        if (smtpPort == 587) {
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.starttls.required"] = "true"
            props["mail.smtp.ssl.enable"] = "false"
            props["mail.smtp.ssl.protocols"] = "TLSv1.2"
        } else if (smtpPort == 465 || useSSL) {
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.starttls.enable"] = "false"
            props["mail.smtp.socketFactory.port"] = smtpPort.toString()
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.socketFactory.fallback"] = "false"
        }

        props["mail.smtp.connectiontimeout"] = "30000"
        props["mail.smtp.timeout"] = "30000"
        props["mail.smtp.writetimeout"] = "30000"

        if (smtpHost.contains("gmail.com")) {
            props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
            props["mail.smtp.ssl.checkserveridentity"] = "true"
        }

        return mailSender
    }
}