package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.domain.model.EmailAttachment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.email.domain.model.*
import com.carslab.crm.modules.email.domain.ports.*
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.domain.services.VisitValidationService
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class EmailSendingService(
    private val emailRepository: EmailRepository,
    private val emailSender: EmailSender,
    private val protocolDataProvider: ProtocolDataProvider,
    private val companySettingsDomainService: CompanySettingsDomainService,
    private val securityContext: SecurityContext,
    private val emailTemplateService: EmailTemplateService,
    private val protocolDocumentStorageService: ProtocolDocumentStorageService
    ) {
    private val logger = LoggerFactory.getLogger(EmailSendingService::class.java)

    fun sendProtocolEmail(
        protocolId: String,
    ): String {
        logger.info("Starting protocol email sending process for protocol: $protocolId")

        val companyId = securityContext.getCurrentCompanyId()

        // Pobierz dane protokołu
        val protocolData = protocolDataProvider.getProtocolData(protocolId)
            ?: throw IllegalArgumentException("Protocol not found: $protocolId")

        // Pobierz ustawienia firmy
        val companySettings = companySettingsDomainService.getCompanySettings(companyId)
            ?: throw IllegalStateException("Company settings not found for company: $companyId")

        // Określ adres odbiorcy
        val finalRecipientEmail = protocolData.clientEmail
        if (finalRecipientEmail.isBlank()) {
            throw IllegalArgumentException("No recipient email address available")
        }

        // Sprawdź konfigurację email
        if (!companySettings.emailSettings.hasValidSmtpConfig()) {
            throw IllegalStateException("Email configuration is not properly set up for company: $companyId")
        }

        val emailId = EmailHistoryId.generate()

        try {
            // Wygeneruj treść emaila z szablonu
            val emailContent = emailTemplateService.generateProtocolEmail(
                protocolData = protocolData,
                companySettings = companySettings,
            )

            // Określ temat
            val subject = emailTemplateService.generateSubject(protocolData)

            // Zapisz w historii jako PENDING
            val emailHistory = EmailHistory(
                id = emailId,
                companyId = companyId,
                protocolId = protocolId,
                recipientEmail = finalRecipientEmail,
                subject = subject,
                htmlContent = emailContent,
                status = EmailStatus.PENDING,
                sentAt = LocalDateTime.now()
            )
            emailRepository.save(emailHistory)

            val attachment = protocolDocumentStorageService.getDocumentData(protocolId)
            val sent = emailSender.sendEmail(
                recipientEmail = companySettings.emailSettings.senderEmail!!,
                subject = subject,
                htmlContent = emailContent,
                senderName = companySettings.emailSettings.senderName,
                senderEmail = companySettings.emailSettings.senderEmail,
                attachment = EmailAttachment(
                    id = UUID.randomUUID().toString(),
                    filename = "CarsLab_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}.pdf",
                    mimeType = "application/pdf\n",
                    size = attachment!!.size,
                    data = attachment
                )
            )

            // Aktualizuj status
            if (sent) {
                emailRepository.updateStatus(emailId, EmailStatus.SENT)
                logger.info("Successfully sent protocol email for protocol: $protocolId, emailId: ${emailId.value}")
            } else {
                emailRepository.updateStatus(emailId, EmailStatus.FAILED, "Failed to send email")
                logger.error("Failed to send protocol email for protocol: $protocolId, emailId: ${emailId.value}")
                throw RuntimeException("Failed to send email")
            }

            return emailId.value
        } catch (e: Exception) {
            logger.error("Error sending protocol email for protocol: $protocolId", e)
            emailRepository.updateStatus(emailId, EmailStatus.FAILED, e.message)
            throw e
        }
    }
}