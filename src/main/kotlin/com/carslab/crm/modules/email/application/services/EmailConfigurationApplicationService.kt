package com.carslab.crm.modules.email.application.services

import com.carslab.crm.modules.email.api.configuration.requests.SaveEmailConfigurationRequest
import com.carslab.crm.modules.email.api.configuration.responses.EmailConfigurationResponse
import com.carslab.crm.modules.email.api.configuration.responses.EmailSuggestionsResponse
import com.carslab.crm.modules.email.domain.model.CreateEmailConfiguration
import com.carslab.crm.modules.email.domain.services.EmailConfigurationDomainService
import com.carslab.crm.modules.email.domain.services.EmailProviderService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmailConfigurationApplicationService(
    private val emailConfigurationDomainService: EmailConfigurationDomainService,
    private val emailProviderService: EmailProviderService
) {
    private val logger = LoggerFactory.getLogger(EmailConfigurationApplicationService::class.java)

    fun saveConfiguration(companyId: Long, request: SaveEmailConfigurationRequest): EmailConfigurationResponse {
        logger.info("Saving email configuration for company: $companyId")

        try {
            val createRequest = CreateEmailConfiguration(
                companyId = companyId,
                senderEmail = request.senderEmail,
                senderName = request.senderName,
                plainPassword = request.emailPassword,
                smtpHost = request.smtpHost,
                smtpPort = request.smtpPort,
                useSSL = request.useSSL,
                isEnabled = request.isEnabled,
                sendTestEmail = request.sendTestEmail
            )

            val savedConfiguration = emailConfigurationDomainService.createOrUpdateConfiguration(createRequest)

            logger.info("Successfully saved email configuration for company: $companyId")
            return EmailConfigurationResponse.from(savedConfiguration, request.sendTestEmail)
        } catch (e: Exception) {
            logger.error("Error saving email configuration for company: $companyId", e)
            throw e
        }
    }

    fun getConfigurationSuggestions(email: String): EmailSuggestionsResponse {
        logger.debug("Getting configuration suggestions for email: $email")
        return emailProviderService.getSuggestions(email)
    }

    @Transactional(readOnly = true)
    fun getCurrentConfiguration(companyId: Long): EmailConfigurationResponse? {
        logger.debug("Getting current email configuration for company: $companyId")

        val configuration = emailConfigurationDomainService.getConfiguration(companyId)
        return configuration?.let { EmailConfigurationResponse.from(it) }
    }
}