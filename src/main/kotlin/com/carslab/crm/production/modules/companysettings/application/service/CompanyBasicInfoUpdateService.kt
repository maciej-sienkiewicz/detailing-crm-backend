package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.*
import com.carslab.crm.production.modules.companysettings.domain.command.*
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serwis do aktualizacji podstawowych informacji o firmie
 */
@Service
@Transactional
class CompanyBasicInfoUpdateService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
) {
    private val logger = LoggerFactory.getLogger(CompanyBasicInfoUpdateService::class.java)

    fun updateBasicInfo(companyId: Long, request: UpdateBasicInfoRequest): CompanySettingsResponse {
        logger.info("Updating basic info for company ID: $companyId")

        val command = UpdateBasicInfoCommand(
            companyId = companyId,
            companyName = request.companyName,
            address = request.address,
            phone = request.phone,
            website = request.website
        )

        val updatedCompany = companyDomainService.updateBasicInfo(command)

        logger.info("Successfully updated basic info for company ID: $companyId")
        return CompanySettingsResponse.from(updatedCompany, logoStorageService, googleDriveConfigurationService)
    }
}