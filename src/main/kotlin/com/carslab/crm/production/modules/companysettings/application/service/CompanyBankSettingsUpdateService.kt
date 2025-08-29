package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.application.dto.UpdateBankSettingsRequest
import com.carslab.crm.production.modules.companysettings.domain.command.UpdateBankSettingsCommand
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class CompanyBankSettingsUpdateService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
) {
    private val logger = LoggerFactory.getLogger(CompanyBankSettingsUpdateService::class.java)

    fun updateBankSettings(companyId: Long, request: UpdateBankSettingsRequest): CompanySettingsResponse {
        logger.info("Updating bank settings for company ID: $companyId")

        val command = UpdateBankSettingsCommand(
            companyId = companyId,
            bankAccountNumber = request.bankAccountNumber,
            bankName = request.bankName,
            swiftCode = request.swiftCode,
            accountHolderName = request.accountHolderName
        )

        val updatedCompany = companyDomainService.updateBankSettings(command)

        logger.info("Successfully updated bank settings for company ID: $companyId")
        return CompanySettingsResponse.from(updatedCompany, logoStorageService, googleDriveConfigurationService)
    }
}