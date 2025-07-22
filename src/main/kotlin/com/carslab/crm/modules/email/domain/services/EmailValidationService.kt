package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.modules.email.domain.model.ValidationStatus
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import java.util.*
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.MessagingException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

@Service
class EmailValidationService {

    private val logger = LoggerFactory.getLogger(EmailValidationService::class.java)

    data class ValidationResult(
        val status: ValidationStatus,
        val message: String?
    )

    fun validateConfiguration(
        senderEmail: String,
        senderName: String,
        password: String,
        smtpHost: String,
        smtpPort: Int,
        useSSL: Boolean,
        sendTestEmail: Boolean = false
    ): ValidationResult {
        return try {
            val mailSender = createMailSender(senderEmail, password, smtpHost, smtpPort, useSSL)

            if (sendTestEmail) {
                sendTestEmail(mailSender, senderEmail, senderName)
            } else {
                testConnection(mailSender)
            }

            ValidationResult(
                status = ValidationStatus.VALID,
                message = "Email configuration is valid"
            )
        } catch (e: AuthenticationFailedException) {
            logger.error("Authentication failed for email configuration", e)
            ValidationResult(
                status = ValidationStatus.INVALID_CREDENTIALS,
                message = "Invalid email or password"
            )
        } catch (e: MessagingException) {
            logger.error("SMTP connection error", e)
            val message = when {
                e.message?.contains("Connection refused") == true -> "Unable to connect to SMTP server"
                e.message?.contains("Unknown host") == true -> "Invalid SMTP server hostname"
                else -> "SMTP server configuration error"
            }
            ValidationResult(
                status = ValidationStatus.CONNECTION_ERROR,
                message = message
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during email validation", e)
            ValidationResult(
                status = ValidationStatus.INVALID_SETTINGS,
                message = "Configuration validation failed: ${e.message}"
            )
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

        if (smtpPort == 587 || !useSSL) {
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.starttls.required"] = "true"
            props["mail.smtp.ssl.enable"] = "false"
        } else if (smtpPort == 465 || useSSL) {
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.starttls.enable"] = "false"
            props["mail.smtp.socketFactory.port"] = smtpPort.toString()
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.socketFactory.fallback"] = "false"
        }

        props["mail.smtp.connectiontimeout"] = "10000"
        props["mail.smtp.timeout"] = "10000"
        props["mail.smtp.writetimeout"] = "10000"

        if (smtpHost.contains("gmail.com")) {
            props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
            props["mail.smtp.ssl.protocols"] = "TLSv1.2"
        }

        return mailSender
    }

    private fun testConnection(mailSender: JavaMailSenderImpl) {
        val transport = mailSender.javaMailProperties.getProperty("mail.transport.protocol")
        val session = mailSender.session
        val transportObj = session.getTransport(transport)

        transportObj.use { t ->
            t.connect(
                mailSender.host,
                mailSender.port,
                mailSender.username,
                mailSender.password
            )
        }
    }

    private fun sendTestEmail(mailSender: JavaMailSenderImpl, senderEmail: String, senderName: String) {
        val message: MimeMessage = mailSender.createMimeMessage()

        message.setFrom(InternetAddress(senderEmail, senderName))
        message.setRecipients(MimeMessage.RecipientType.TO, senderEmail)
        message.subject = "Email Configuration Test"
        message.setText("This is a test email to verify your email configuration is working correctly.")

        mailSender.send(message)
    }
}