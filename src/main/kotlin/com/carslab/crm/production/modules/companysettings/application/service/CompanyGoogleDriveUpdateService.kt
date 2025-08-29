package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.*
import com.carslab.crm.production.modules.companysettings.domain.command.*
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanyGoogleDriveUpdateService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
) {
    private val logger = LoggerFactory.getLogger(CompanyGoogleDriveUpdateService::class.java)

    fun updateGoogleDriveSettings(companyId: Long, request: UpdateGoogleDriveSettingsRequest): CompanySettingsResponse {
        logger.info("Updating Google Drive settings for company ID: $companyId")

        val command = UpdateGoogleDriveSettingsCommand(
            companyId = companyId,
            folderId = request.folderId,
            folderName = request.folderName,
            enabled = request.enabled,
            autoUploadInvoices = request.autoUploadInvoices,
            autoCreateFolders = request.autoCreateFolders
        )

        val updatedCompany = companyDomainService.updateGoogleDriveSettings(command)

        logger.info("Successfully updated Google Drive settings for company ID: $companyId")
        return CompanySettingsResponse.from(updatedCompany, logoStorageService, googleDriveConfigurationService)
    }
}