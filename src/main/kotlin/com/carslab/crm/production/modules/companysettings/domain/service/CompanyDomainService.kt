package com.carslab.crm.production.modules.companysettings.domain.service

import com.carslab.crm.api.model.commands.CreateCalendarColorCommand
import com.carslab.crm.domain.settings.CalendarColorFacade
import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.domain.command.*
import com.carslab.crm.production.modules.companysettings.domain.model.*
import com.carslab.crm.production.modules.companysettings.domain.repository.CompanyRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.CompanyNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CompanyDomainService(
    private val repository: CompanyRepository,
    private val logoStorageService: LogoStorageService,
    private val googleDriveConfigurationService: GoogleDriveConfigurationService,
    private val colorFacade: CalendarColorFacade
) {

    fun createCompany(command: CreateCompanyCommand): Company {
        if (repository.existsByTaxId(command.taxId)) {
            throw BusinessException("Company with tax ID ${command.taxId} already exists")
        }

        val company = Company(
            id = CompanyId.of(0),
            name = command.companyName,
            taxId = command.taxId,
            address = command.address,
            phone = command.phone,
            website = command.website,
            logoId = null,
            bankSettings = BankSettings(
                bankAccountNumber = command.bankAccountNumber,
                bankName = command.bankName,
                swiftCode = command.swiftCode,
                accountHolderName = command.accountHolderName
            ),
            mailConfiguration = MailConfiguration(),
            googleDriveSettings = googleDriveConfigurationService.initializeGoogleDriveCredentials(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        return repository.save(company)
            .also { colorFacade.createCalendarColor(
                CreateCalendarColorCommand(
                    name = "Domyślny kolor",
                    color = "#1a365d",
                ),
                companyId = it.id.value
            ) }
    }

    fun getCompanyById(companyId: Long): Company {
        return repository.findById(companyId)
            ?: throw CompanyNotFoundException("Company with ID $companyId not found")
    }

    fun uploadLogo(command: UploadLogoCommand): Company {
        val company = getCompanyById(command.companyId)

        company.logoId?.let { oldLogoId ->
            logoStorageService.deleteLogo(oldLogoId)
        }

        val logoMetadata = logoStorageService.storeLogo(command.companyId, command.logoFile)

        val updatedCompany = company.copy(
            logoId = logoMetadata.fileId,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }

    /**
     * Aktualizuje podstawowe informacje o firmie
     */
    fun updateBasicInfo(command: UpdateBasicInfoCommand): Company {
        val company = getCompanyById(command.companyId)

        val updatedCompany = company.copy(
            name = command.companyName,
            address = command.address,
            phone = command.phone,
            website = command.website,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }

    /**
     * Aktualizuje dane bankowe firmy
     */
    fun updateBankSettings(command: UpdateBankSettingsCommand): Company {
        val company = getCompanyById(command.companyId)

        val updatedBankSettings = company.bankSettings.copy(
            bankAccountNumber = command.bankAccountNumber,
            bankName = command.bankName,
            swiftCode = command.swiftCode,
            accountHolderName = command.accountHolderName
        )

        val updatedCompany = company.copy(
            bankSettings = updatedBankSettings,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }

    /**
     * Aktualizuje konfigurację mailową
     */
    fun updateMailConfiguration(command: UpdateMailConfigurationCommand): Company {
        val company = getCompanyById(command.companyId)

        val currentMailConfig = company.mailConfiguration
        val updatedMailConfiguration = MailConfiguration(
            smtpServer = command.smtpServer ?: currentMailConfig.smtpServer,
            smtpPort = command.smtpPort ?: currentMailConfig.smtpPort,
            email = command.email ?: currentMailConfig.email,
            emailPassword = command.emailPassword ?: currentMailConfig.emailPassword,
            useTls = command.useTls ?: currentMailConfig.useTls,
            useSsl = command.useSsl ?: currentMailConfig.useSsl,
            fromName = command.fromName ?: currentMailConfig.fromName,
            enabled = command.enabled ?: currentMailConfig.enabled
        )

        val updatedCompany = company.copy(
            mailConfiguration = updatedMailConfiguration,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }

    /**
     * Aktualizuje ustawienia Google Drive
     * Client credentials są pobierane z properties - aktualizuje tylko folder i opcje
     */
    fun updateGoogleDriveSettings(command: UpdateGoogleDriveSettingsCommand): Company {
        val company = getCompanyById(command.companyId)

        // Sprawdzamy czy globalna konfiguracja Google Drive jest poprawna
        if (!googleDriveConfigurationService.isConfigurationValid()) {
            throw BusinessException("Google Drive configuration is not properly set in application properties")
        }

        val currentSettings = company.googleDriveSettings
        val updatedGoogleDriveSettings = GoogleDriveSettings(
            clientId = currentSettings.clientId, // pozostaje bez zmian - z properties
            clientSecret = currentSettings.clientSecret, // pozostaje bez zmian - z properties
            refreshToken = currentSettings.refreshToken, // pozostaje bez zmian - z properties
            defaultFolderId = command.folderId, // nowa wartość
            defaultFolderName = command.folderName, // nowa wartość
            enabled = command.enabled ?: currentSettings.enabled,
            autoUploadInvoices = command.autoUploadInvoices ?: currentSettings.autoUploadInvoices,
            autoCreateFolders = command.autoCreateFolders ?: currentSettings.autoCreateFolders
        )

        val updatedCompany = company.copy(
            googleDriveSettings = updatedGoogleDriveSettings,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }
}