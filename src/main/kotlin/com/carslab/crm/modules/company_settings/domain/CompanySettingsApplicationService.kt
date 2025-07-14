package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.modules.company_settings.api.requests.CreateCompanySettingsRequest
import com.carslab.crm.modules.company_settings.api.requests.UpdateCompanySettingsRequest
import com.carslab.crm.modules.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.modules.company_settings.domain.model.BankSettings
import com.carslab.crm.modules.company_settings.domain.model.CompanyBasicInfo
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CreateCompanySettings
import com.carslab.crm.modules.company_settings.domain.model.EmailSettings
import com.carslab.crm.modules.company_settings.domain.model.LogoSettings
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class CompanySettingsApplicationService(
    private val companySettingsDomainService: CompanySettingsDomainService,
    private val logoStorageService: LogoStorageService,
    private val securityContext: SecurityContext,
) {
    private val logger = LoggerFactory.getLogger(CompanySettingsApplicationService::class.java)

    fun createCompanySettings(request: CreateCompanySettingsRequest): CompanySettingsResponse {
        logger.info("Creating company settings for company: ${request.companyId}")
        try {
            validateCreateRequest(request)

            val companyId = securityContext.getCurrentCompanyId()
            val createDomain = CreateCompanySettings(
                companyId = companyId,
                basicInfo = CompanyBasicInfo(
                    companyName = request.companyName,
                    taxId = request.taxId,
                    address = request.address,
                    phone = request.phone,
                    website = request.website
                ),
                bankSettings = BankSettings(
                    bankAccountNumber = request.bankAccountNumber,
                    bankName = request.bankName,
                    swiftCode = request.swiftCode,
                    accountHolderName = request.accountHolderName
                ),
                emailSettings = EmailSettings(
                    smtpHost = request.smtpHost,
                    smtpPort = request.smtpPort,
                    smtpUsername = request.smtpUsername,
                    smtpPassword = request.smtpPassword,
                    imapHost = request.imapHost,
                    imapPort = request.imapPort,
                    imapUsername = request.imapUsername,
                    imapPassword = request.imapPassword,
                    senderEmail = request.senderEmail,
                    senderName = request.senderName,
                    useSSL = request.useSSL ?: true,
                    useTLS = request.useTLS ?: true
                )
            )

            val companySettings = companySettingsDomainService.createCompanySettings(createDomain)

            logger.info("Successfully created company settings with ID: ${companySettings.id.value}")
            return CompanySettingsResponse.from(companySettings)
        } catch (e: DomainException) {
            logger.error("Failed to create company settings: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating company settings", e)
            throw RuntimeException("Failed to create company settings", e)
        }
    }

    fun updateCompanySettings(companyId: Long, request: UpdateCompanySettingsRequest): CompanySettingsResponse {
        logger.info("Updating company settings for company: $companyId")

        try {
            validateUpdateRequest(request)

            val companySettings = companySettingsDomainService.updateCompanySettings(companyId, request)

            logger.info("Successfully updated company settings for company: $companyId")
            return CompanySettingsResponse.from(companySettings)
        } catch (e: DomainException) {
            logger.error("Failed to update company settings for company $companyId: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating company settings for company $companyId", e)
            throw RuntimeException("Failed to update company settings", e)
        }
    }

    @Transactional(readOnly = true)
    fun getCompanySettings(companyId: Long): CompanySettingsResponse {
        logger.debug("Getting company settings for company: $companyId")

        val companySettings = companySettingsDomainService.getCompanySettings(companyId)

        return companySettings?.let { CompanySettingsResponse.from(it) } ?: CompanySettingsResponse(
            id = null,
            companyId = null,
            basicInfo = null,
            bankSettings = null,
            emailSettings = null,
            logoSettings = null,
            createdAt = null,
            updatedAt = null
        )
    }

    /**
     * Uploads logo for a company. If company settings don't exist, creates them with default values.
     * This ensures that logo can be uploaded at any time, even for new companies.
     */
    fun uploadLogo(companyId: Long, logoFile: MultipartFile): CompanySettingsResponse {
        logger.info("Uploading logo for company: $companyId")

        try {
            validateLogoFile(logoFile)

            // Get existing settings or create defaults if they don't exist
            val existingSettings = getOrCreateDefaultCompanySettings(companyId)

            // Remove previous logo if it exists
            existingSettings.logoSettings.logoFileId?.let { oldLogoId ->
                try {
                    logoStorageService.deleteLogo(oldLogoId)
                    logger.debug("Deleted previous logo: $oldLogoId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete previous logo: $oldLogoId", e)
                    // Continue with upload even if deletion fails
                }
            }

            // Store new logo
            val logoMetadata = logoStorageService.storeLogo(companyId, logoFile)
            logger.debug("Stored new logo with ID: ${logoMetadata.fileId}")

            // Update settings with new logo information
            val updateRequest = UpdateCompanySettingsRequest(
                basicInfo = existingSettings.basicInfo,
                bankSettings = existingSettings.bankSettings,
                emailSettings = existingSettings.emailSettings,
                logoSettings = LogoSettings(
                    logoFileId = logoMetadata.fileId,
                    logoFileName = logoMetadata.fileName,
                    logoContentType = logoMetadata.contentType,
                    logoSize = logoMetadata.size,
                    logoUrl = logoMetadata.url
                )
            )

            val updatedSettings = companySettingsDomainService.updateCompanySettings(companyId, updateRequest)

            logger.info("Successfully uploaded logo for company: $companyId")
            return CompanySettingsResponse.from(updatedSettings)
        } catch (e: Exception) {
            logger.error("Failed to upload logo for company $companyId", e)
            throw RuntimeException("Failed to upload logo", e)
        }
    }

    fun deleteLogo(companyId: Long): CompanySettingsResponse {
        logger.info("Deleting logo for company: $companyId")

        try {
            val existingSettings = getOrCreateDefaultCompanySettings(companyId)

            // Delete logo file if it exists
            existingSettings.logoSettings.logoFileId?.let { logoFileId ->
                try {
                    logoStorageService.deleteLogo(logoFileId)
                    logger.debug("Deleted logo file: $logoFileId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete logo file: $logoFileId", e)
                    // Continue with settings update even if file deletion fails
                }
            }

            // Update settings to remove logo information
            val updateRequest = UpdateCompanySettingsRequest(
                basicInfo = existingSettings.basicInfo,
                bankSettings = existingSettings.bankSettings,
                emailSettings = existingSettings.emailSettings,
                logoSettings = LogoSettings() // Empty logo settings
            )

            val updatedSettings = companySettingsDomainService.updateCompanySettings(companyId, updateRequest)

            logger.info("Successfully deleted logo for company: $companyId")
            return CompanySettingsResponse.from(updatedSettings)
        } catch (e: Exception) {
            logger.error("Failed to delete logo for company $companyId", e)
            throw RuntimeException("Failed to delete logo", e)
        }
    }

    fun deleteCompanySettings(companyId: Long): Boolean {
        logger.info("Deleting company settings for company: $companyId")

        val existingSettings = companySettingsDomainService.getCompanySettings(companyId)
        existingSettings?.logoSettings?.logoFileId?.let { logoFileId ->
            try {
                logoStorageService.deleteLogo(logoFileId)
                logger.debug("Deleted logo file during settings deletion: $logoFileId")
            } catch (e: Exception) {
                logger.warn("Failed to delete logo file during settings deletion: $logoFileId", e)
                // Continue with settings deletion even if logo deletion fails
            }
        }

        val deleted = companySettingsDomainService.deleteCompanySettings(companyId)

        if (deleted) {
            logger.info("Successfully deleted company settings for company: $companyId")
        } else {
            logger.warn("Company settings for company: $companyId not found for deletion")
        }

        return deleted
    }

    /**
     * Gets existing company settings or creates default ones if they don't exist.
     * This ensures that operations like logo upload can work even for new companies.
     */
    private fun getOrCreateDefaultCompanySettings(companyId: Long): CompanySettings {
        return companySettingsDomainService.getCompanySettings(companyId)
            ?: createDefaultCompanySettings(companyId)
    }

    /**
     * Creates default company settings for a new company.
     * Uses placeholder values that can be updated later by the administrator.
     */
    private fun createDefaultCompanySettings(companyId: Long): CompanySettings {
        logger.info("Creating default company settings for company: $companyId")

        val defaultCreateRequest = CreateCompanySettings(
            companyId = companyId,
            basicInfo = CompanyBasicInfo(
                companyName = "Your Company", // Placeholder - to be updated by admin
                taxId = "0000000000", // Placeholder - to be updated by admin
                address = null,
                phone = null,
                website = null
            ),
            bankSettings = BankSettings(),
            emailSettings = EmailSettings(),
            logoSettings = LogoSettings()
        )

        return companySettingsDomainService.createCompanySettingsWithDefaults(defaultCreateRequest)
    }

    private fun validateCreateRequest(request: CreateCompanySettingsRequest) {
        require(request.companyName.isNotBlank()) { "Company name cannot be blank" }
        require(request.taxId.isNotBlank()) { "Tax ID cannot be blank" }
    }

    private fun validateUpdateRequest(request: UpdateCompanySettingsRequest) {
        require(request.basicInfo.companyName.isNotBlank()) { "Company name cannot be blank" }
        require(request.basicInfo.taxId.isNotBlank()) { "Tax ID cannot be blank" }
    }

    private fun validateLogoFile(file: MultipartFile) {
        require(!file.isEmpty) { "Logo file cannot be empty" }
        require(file.size <= 5 * 1024 * 1024) { "Logo file size cannot exceed 5MB" }

        val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/svg+xml")
        require(file.contentType in allowedTypes) {
            "Logo file must be an image (JPEG, PNG, GIF, or SVG)"
        }
    }
}