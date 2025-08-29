package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.domain.command.UploadLogoCommand
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class CompanyLogoService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
) {
    private val logger = LoggerFactory.getLogger(CompanyLogoService::class.java)

    fun uploadLogo(companyId: Long, logoFile: MultipartFile): CompanySettingsResponse {
        logger.info("Uploading logo for company ID: $companyId, file: ${logoFile.originalFilename}")

        validateLogoFile(logoFile)

        val uploadCommand = UploadLogoCommand(
            companyId = companyId,
            logoFile = logoFile
        )

        val updatedCompany = companyDomainService.uploadLogo(uploadCommand)

        logger.info("Successfully uploaded logo for company ID: $companyId, logoId: ${updatedCompany.logoId}")

        return CompanySettingsResponse.from(updatedCompany, logoStorageService, googleDriveConfigurationService)
    }

    private fun validateLogoFile(logoFile: MultipartFile) {
        if (logoFile.isEmpty) {
            throw IllegalArgumentException("Logo file cannot be empty")
        }

        val allowedContentTypes = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
        )

        if (logoFile.contentType !in allowedContentTypes) {
            throw IllegalArgumentException(
                "Invalid file type. Allowed types: ${allowedContentTypes.joinToString(", ")}"
            )
        }

        val maxFileSize = 5 * 1024 * 1024L // 5MB
        if (logoFile.size > maxFileSize) {
            throw IllegalArgumentException("File size cannot exceed 5MB")
        }
    }
}