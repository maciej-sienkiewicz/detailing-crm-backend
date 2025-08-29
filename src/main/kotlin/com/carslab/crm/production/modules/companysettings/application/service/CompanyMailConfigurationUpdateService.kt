package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.application.dto.UpdateBankSettingsRequest
import com.carslab.crm.production.modules.companysettings.application.dto.UpdateMailConfigurationRequest
import com.carslab.crm.production.modules.companysettings.domain.command.UpdateBankSettingsCommand
import com.carslab.crm.production.modules.companysettings.domain.command.UpdateMailConfigurationCommand
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class CompanyMailConfigurationUpdateService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
) {
    private val logger = LoggerFactory.getLogger(CompanyMailConfigurationUpdateService::class.java)

    fun updateMailConfiguration(companyId: Long, request: UpdateMailConfigurationRequest): CompanySettingsResponse {
        logger.info("Updating mail configuration for company ID: $companyId")

        val command = UpdateMailConfigurationCommand(
            companyId = companyId,
            smtpServer = request.smtpServer,
            smtpPort = request.smtpPort,
            email = request.email,
            emailPassword = request.emailPassword,
            useTls = request.useTls,
            useSsl = request.useSsl,
            fromName = request.fromName,
            enabled = request.enabled
        )

        val updatedCompany = companyDomainService.updateMailConfiguration(command)

        logger.info("Successfully updated mail configuration for company ID: $companyId")
        return CompanySettingsResponse.from(updatedCompany, logoStorageService, googleDriveConfigurationService)
    }
}