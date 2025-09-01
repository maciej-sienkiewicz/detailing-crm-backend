package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentType
import com.carslab.crm.modules.email.domain.model.*
import com.carslab.crm.modules.email.domain.ports.*
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.companysettings.domain.repository.CompanyRepository
import com.carslab.crm.production.modules.visits.application.service.query.VisitDocumentQueryService
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
    private val companyDetailsFetcher: CompanyDetailsFetchService,
    private val securityContext: SecurityContext,
    private val emailTemplateService: EmailTemplateService,
    private val documentService: UniversalStorageService,
    private val visitDocumentQueryService: VisitDocumentQueryService,
) {
    private val logger = LoggerFactory.getLogger(EmailSendingService::class.java)

    fun sendProtocolEmail(
        protocolId: String,
        recipientEmail: String? = null,
        customSubject: String? = null,
        additionalVariables: Map<String, String> = emptyMap()
    ): String {
        logger.info("Starting protocol email sending process for protocol: $protocolId")

        val companyId = securityContext.getCurrentCompanyId()

        val protocolData = protocolDataProvider.getProtocolData(protocolId)
            ?: throw IllegalArgumentException("Protocol not found: $protocolId")
        
        if(!listOf("crm1@sienkiewicz-maciej.pl", "crm2@sienkiewicz-maciej.pl", "crm3@sienkiewicz-maciej.pl").contains(protocolData.clientEmail)) {
            logger.info("Client email ${protocolData.clientEmail} is not in the allowed list, skipping email sending.")
            return "skipped"
        }

        val emailConfig = companyDetailsFetcher.getCompanySettings(companyId)
            .mailConfiguration

        if (!emailConfig.enabled) {
            throw IllegalStateException("Email configuration is disabled for company: $companyId")
        }

        val finalRecipientEmail = recipientEmail ?: protocolData.clientEmail
        if (finalRecipientEmail.isBlank()) {
            throw IllegalArgumentException("No recipient email address available")
        }

        val emailId = EmailHistoryId.generate()

        try {
            val emailContent = emailTemplateService.generateProtocolEmail(
                protocolData = protocolData,
                companyName = emailConfig.fromName ?: throw IllegalStateException("Sender name not configured"),
                companyEmail = emailConfig.email ?: throw IllegalStateException("Sender email not configured"),
                additionalVariables = additionalVariables
            )

            val subject = customSubject ?: emailTemplateService.generateSubject(protocolData)

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


            val protocolDetails = visitDocumentQueryService.getVisitDocuments(protocolId)
                .filter { it.documentType == ProtocolDocumentType.ACCEPTANCE_PROTOCOL.toString() }
                .maxBy { it.createdAt }
            
            val protocol: ByteArray? = if(protocolDetails != null) {
                logger.info("Found protocol document: ${protocolDetails.protocolId} for protocol: $protocolId")
                visitDocumentQueryService.findByVisitIdAndDocumentType(
                    protocolId, ProtocolDocumentType.ACCEPTANCE_PROTOCOL.toString()
                )
            } else {
                logger.warn("No protocol document found for protocol: $protocolId")
                null
            }
            
            val sent = emailSender.sendEmail(
                recipientEmail = finalRecipientEmail,
                subject = subject,
                htmlContent = emailContent,
                senderName = emailConfig.fromName,
                senderEmail = emailConfig.email,
                attachment = if (protocol != null) EmailAttachment(
                    filename = protocolDetails.originalName,
                    content = protocol,
                    contentType = "application/pdf"
                ) else null
            )

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