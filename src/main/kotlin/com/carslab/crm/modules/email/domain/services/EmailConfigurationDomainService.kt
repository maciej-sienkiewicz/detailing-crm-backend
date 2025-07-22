package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.modules.email.domain.model.CreateEmailConfiguration
import com.carslab.crm.modules.email.domain.model.EmailConfiguration
import com.carslab.crm.modules.email.domain.model.ValidationStatus
import com.carslab.crm.modules.email.domain.ports.EmailConfigurationRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmailConfigurationDomainService(
    private val emailConfigurationRepository: EmailConfigurationRepository,
    private val emailValidationService: EmailValidationService,
    private val emailProviderService: EmailProviderService,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(EmailConfigurationDomainService::class.java)

    fun createOrUpdateConfiguration(createRequest: CreateEmailConfiguration): EmailConfiguration {
        logger.info("Creating/updating email configuration for company: ${createRequest.companyId}")

        validateRequest(createRequest)

        val validationResult = emailValidationService.validateConfiguration(
            senderEmail = createRequest.senderEmail,
            senderName = createRequest.senderName,
            password = createRequest.plainPassword,
            smtpHost = createRequest.smtpHost,
            smtpPort = createRequest.smtpPort,
            useSSL = createRequest.useSSL,
            sendTestEmail = createRequest.sendTestEmail
        )

        val encryptedPassword = encryptionService.encrypt(createRequest.plainPassword)
        val providerHint = emailProviderService.getProviderHint(createRequest.senderEmail)

        val configuration = EmailConfiguration(
            id = null,
            companyId = createRequest.companyId,
            senderEmail = createRequest.senderEmail,
            senderName = createRequest.senderName,
            encryptedPassword = encryptedPassword,
            smtpHost = createRequest.smtpHost,
            smtpPort = createRequest.smtpPort,
            useSSL = createRequest.useSSL,
            isEnabled = createRequest.isEnabled,
            validationStatus = validationResult.status,
            validationMessage = validationResult.message,
            providerHint = providerHint
        )

        return emailConfigurationRepository.saveOrUpdate(configuration)
    }

    @Transactional(readOnly = true)
    fun getConfiguration(companyId: Long): EmailConfiguration? {
        return emailConfigurationRepository.findByCompanyId(companyId)
    }

    fun deleteConfiguration(companyId: Long): Boolean {
        return emailConfigurationRepository.deleteByCompanyId(companyId)
    }

    private fun validateRequest(request: CreateEmailConfiguration) {
        require(request.senderEmail.isNotBlank()) { "Sender email cannot be blank" }
        require(request.senderName.isNotBlank()) { "Sender name cannot be blank" }
        require(request.plainPassword.isNotBlank()) { "Password cannot be blank" }
        require(request.smtpHost.isNotBlank()) { "SMTP host cannot be blank" }
        require(request.smtpPort in 1..65535) { "SMTP port must be between 1 and 65535" }
        require(isValidEmail(request.senderEmail)) { "Invalid email format" }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }
}