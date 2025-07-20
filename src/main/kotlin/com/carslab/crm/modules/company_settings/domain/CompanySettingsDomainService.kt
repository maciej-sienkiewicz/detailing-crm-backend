package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.modules.company_settings.api.requests.UpdateCompanySettingsRequest
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.modules.company_settings.domain.model.CreateCompanySettings
import com.carslab.crm.modules.company_settings.domain.model.EmailSettings
import com.carslab.crm.modules.company_settings.domain.port.CompanySettingsRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.modules.company_settings.domain.model.BankSettings
import com.carslab.crm.modules.company_settings.domain.model.LogoSettings
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanySettingsDomainService(
    private val companySettingsRepository: CompanySettingsRepository,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(CompanySettingsDomainService::class.java)

    /**
     * Creates company settings with full validation.
     * Used when administrator explicitly creates settings.
     */
    fun createCompanySettings(createRequest: CreateCompanySettings): CompanySettings {
        logger.debug("Creating company settings with full validation for company: ${createRequest.companyId}")

        // Check if settings already exist
        if (companySettingsRepository.existsByCompanyId(createRequest.companyId)) {
            throw DomainException("Company settings already exist for company: ${createRequest.companyId}")
        }

        validateBasicInfo(createRequest.basicInfo.companyName, createRequest.basicInfo.taxId)

        val settingsWithEncryptedPasswords = createRequest.copy(
            emailSettings = encryptEmailPasswords(createRequest.emailSettings)
        )

        val savedSettings = companySettingsRepository.saveNew(settingsWithEncryptedPasswords)
        logger.info("Created company settings with ID: ${savedSettings.id.value} for company: ${createRequest.companyId}")

        return savedSettings
    }

    /**
     * Creates company settings with default/placeholder values.
     * Used internally when settings are needed but don't exist (e.g., for logo upload).
     * Does not perform strict validation as these are temporary placeholder values.
     */
    fun createCompanySettingsWithDefaults(createRequest: CreateCompanySettings): CompanySettings {
        logger.debug("Creating default company settings for company: ${createRequest.companyId}")

        // Check if settings already exist
        if (companySettingsRepository.existsByCompanyId(createRequest.companyId)) {
            throw DomainException("Company settings already exist for company: ${createRequest.companyId}")
        }

        // For default settings, we use minimal validation
        require(createRequest.basicInfo.companyName.isNotBlank()) { "Company name cannot be blank" }
        require(createRequest.basicInfo.taxId.isNotBlank()) { "Tax ID cannot be blank" }

        val settingsWithEncryptedPasswords = createRequest.copy(
            emailSettings = encryptEmailPasswords(createRequest.emailSettings)
        )

        val savedSettings = companySettingsRepository.saveNew(settingsWithEncryptedPasswords)
        logger.info("Created default company settings with ID: ${savedSettings.id.value} for company: ${createRequest.companyId}")

        return savedSettings
    }

    fun updateCompanySettings(companyId: Long, updateRequest: UpdateCompanySettingsRequest): CompanySettings {
        logger.debug("Updating company settings for company: $companyId")

        val existingSettings = companySettingsRepository.findByCompanyId(companyId)
            ?: companySettingsRepository.saveNew(
                CreateCompanySettings(
                    companyId = companyId,
                    basicInfo = updateRequest.basicInfo,
                    bankSettings = updateRequest.bankSettings ?: BankSettings(),
                    emailSettings = encryptEmailPasswords(updateRequest.emailSettings ?: EmailSettings()),
                    logoSettings = updateRequest.logoSettings ?: LogoSettings()
                )
            )

        // Validate basic info only if it's not placeholder values
        if (!isPlaceholderValues(updateRequest.basicInfo.companyName, updateRequest.basicInfo.taxId)) {
            validateBasicInfo(updateRequest.basicInfo.companyName, updateRequest.basicInfo.taxId)
        }

        val updatedSettings = existingSettings.copy(
            basicInfo = updateRequest.basicInfo,
            bankSettings = updateRequest.bankSettings ?: existingSettings.bankSettings,
            emailSettings = encryptEmailPasswords(updateRequest.emailSettings ?: existingSettings.emailSettings),
            logoSettings = updateRequest.logoSettings ?: existingSettings.logoSettings,
            audit = existingSettings.audit.updated()
        )

        val savedSettings = companySettingsRepository.save(updatedSettings)
        logger.info("Updated company settings for company: $companyId")

        return savedSettings
    }

    @Transactional(readOnly = true)
    fun getCompanySettings(companyId: Long): CompanySettings? {
        logger.debug("Getting company settings for company: $companyId")

        val settings = companySettingsRepository.findByCompanyId(companyId)
        return settings?.copy(
            emailSettings = decryptEmailPasswords(settings.emailSettings)
        )
    }

    @Transactional(readOnly = true)
    fun getCompanySettingsById(id: CompanySettingsId): CompanySettings? {
        logger.debug("Getting company settings by ID: ${id.value}")

        val settings = companySettingsRepository.findById(id)
        return settings?.copy(
            emailSettings = decryptEmailPasswords(settings.emailSettings)
        )
    }

    fun deleteCompanySettings(companyId: Long): Boolean {
        logger.info("Deleting company settings for company: $companyId")

        val deleted = companySettingsRepository.deleteByCompanyId(companyId)
        if (deleted) {
            logger.info("Successfully deleted company settings for company: $companyId")
        } else {
            logger.warn("Company settings not found for deletion: $companyId")
        }

        return deleted
    }

    /**
     * Checks if the provided values are placeholder values that were used during default creation.
     */
    private fun isPlaceholderValues(companyName: String, taxId: String): Boolean {
        return companyName == "Your Company" && taxId == "0000000000"
    }

    /**
     * Validates basic company information with strict business rules.
     */
    private fun validateBasicInfo(companyName: String, taxId: String) {
        require(companyName.isNotBlank()) { "Company name cannot be blank" }
        require(companyName.length <= 200) { "Company name cannot exceed 200 characters" }
        require(companyName.trim() != "Your Company") { "Please provide a real company name" }

        require(taxId.isNotBlank()) { "Tax ID (NIP) cannot be blank" }
        require(taxId.length <= 20) { "Tax ID cannot exceed 20 characters" }
        require(taxId.trim() != "0000000000") { "Please provide a valid Tax ID" }
        require(isValidPolishNIP(taxId)) { "Invalid Polish NIP format" }
    }

    /**
     * Validates Polish NIP (Tax ID) format and checksum.
     */
    private fun isValidPolishNIP(nip: String): Boolean {
        val nipDigits = nip.replace("-", "").replace(" ", "")
        if (nipDigits.length != 10 || !nipDigits.all { it.isDigit() }) {
            return false
        }

        // Skip validation for placeholder value
        if (nipDigits == "0000000000") {
            return true
        }

        // NIP checksum validation
        val weights = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        val digits = nipDigits.map { it.toString().toInt() }

        val sum = weights.zip(digits.take(9)).sumOf { it.first * it.second }
        val checkDigit = sum % 11

        return checkDigit == digits[9]
    }

    /**
     * Encrypts email passwords if they are not already encrypted.
     */
    private fun encryptEmailPasswords(emailSettings: EmailSettings): EmailSettings {
        return emailSettings.copy(
            smtpPassword = emailSettings.smtpPassword?.let {
                if (it.isNotBlank() && !encryptionService.isEncrypted(it)) {
                    encryptionService.encrypt(it)
                } else it
            },
            imapPassword = emailSettings.imapPassword?.let {
                if (it.isNotBlank() && !encryptionService.isEncrypted(it)) {
                    encryptionService.encrypt(it)
                } else it
            }
        )
    }

    /**
     * Decrypts email passwords if they are encrypted.
     */
    private fun decryptEmailPasswords(emailSettings: EmailSettings): EmailSettings {
        return emailSettings.copy(
            smtpPassword = emailSettings.smtpPassword?.let {
                if (it.isNotBlank() && encryptionService.isEncrypted(it)) {
                    try {
                        encryptionService.decrypt(it)
                    } catch (e: Exception) {
                        logger.warn("Failed to decrypt SMTP password, returning encrypted value", e)
                        it
                    }
                } else it
            },
            imapPassword = emailSettings.imapPassword?.let {
                if (it.isNotBlank() && encryptionService.isEncrypted(it)) {
                    try {
                        encryptionService.decrypt(it)
                    } catch (e: Exception) {
                        logger.warn("Failed to decrypt IMAP password, returning encrypted value", e)
                        it
                    }
                } else it
            }
        )
    }
}