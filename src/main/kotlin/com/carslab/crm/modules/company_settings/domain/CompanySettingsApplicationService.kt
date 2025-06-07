package com.carslab.crm.company_settings.domain

import com.carslab.crm.company_settings.api.requests.CreateCompanySettingsRequest
import com.carslab.crm.company_settings.api.requests.UpdateCompanySettingsRequest
import com.carslab.crm.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.company_settings.domain.model.BankSettings
import com.carslab.crm.company_settings.domain.model.CompanyBasicInfo
import com.carslab.crm.company_settings.domain.model.CreateCompanySettings
import com.carslab.crm.company_settings.domain.model.EmailSettings
import com.carslab.crm.company_settings.domain.model.LogoSettings
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
            ?: throw ResourceNotFoundException("Company settings", companyId)

        return CompanySettingsResponse.from(companySettings)
    }

    fun uploadLogo(companyId: Long, logoFile: MultipartFile): CompanySettingsResponse {
        logger.info("Uploading logo for company: $companyId")

        try {
            validateLogoFile(logoFile)

            val existingSettings = companySettingsDomainService.getCompanySettings(companyId)
                ?: throw ResourceNotFoundException("Company settings", companyId)

            // Usuń poprzednie logo jeśli istnieje
            existingSettings.logoSettings.logoFileId?.let { oldLogoId ->
                logoStorageService.deleteLogo(oldLogoId)
            }

            // Zapisz nowe logo
            val logoMetadata = logoStorageService.storeLogo(companyId, logoFile)

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

        val existingSettings = companySettingsDomainService.getCompanySettings(companyId)
            ?: throw ResourceNotFoundException("Company settings", companyId)

        existingSettings.logoSettings.logoFileId?.let { logoFileId ->
            logoStorageService.deleteLogo(logoFileId)
        }

        val updateRequest = UpdateCompanySettingsRequest(
            basicInfo = existingSettings.basicInfo,
            bankSettings = existingSettings.bankSettings,
            emailSettings = existingSettings.emailSettings,
            logoSettings = LogoSettings()
        )

        val updatedSettings = companySettingsDomainService.updateCompanySettings(companyId, updateRequest)

        logger.info("Successfully deleted logo for company: $companyId")
        return CompanySettingsResponse.from(updatedSettings)
    }

    fun deleteCompanySettings(companyId: Long): Boolean {
        logger.info("Deleting company settings for company: $companyId")

        val existingSettings = companySettingsDomainService.getCompanySettings(companyId)
        existingSettings?.logoSettings?.logoFileId?.let { logoFileId ->
            logoStorageService.deleteLogo(logoFileId)
        }

        val deleted = companySettingsDomainService.deleteCompanySettings(companyId)

        if (deleted) {
            logger.info("Successfully deleted company settings for company: $companyId")
        } else {
            logger.warn("Company settings for company: $companyId not found for deletion")
        }

        return deleted
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