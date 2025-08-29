package com.carslab.crm.production.modules.companysettings.application.dto

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfigurationService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CompanyResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfoResponse,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettingsResponse,

    @JsonProperty("mail_configuration")
    val mailConfiguration: MailConfigurationResponse,

    @JsonProperty("google_drive_settings")
    val googleDriveSettings: GoogleDriveSettingsResponse,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            company: Company,
            logoStorageService: LogoStorageService,
            googleDriveConfigurationService: GoogleDriveConfigurationService
        ): CompanyResponse = CompanyResponse(
            id = company.id.value,
            basicInfo = CompanyBasicInfoResponse(
                companyName = company.name,
                taxId = company.taxId,
                address = company.address,
                phone = company.phone,
                website = company.website,
                logoId = company.logoId,
                logoUrl = company.logoId?.let { logoStorageService.getLogoUrl(it) }
            ),
            bankSettings = BankSettingsResponse.from(company.bankSettings),
            mailConfiguration = MailConfigurationResponse.from(company.mailConfiguration),
            googleDriveSettings = GoogleDriveSettingsResponse.from(
                company.googleDriveSettings,
                googleDriveConfigurationService
            ),
            createdAt = company.createdAt,
            updatedAt = company.updatedAt
        )
    }
}

data class CompanySettingsResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfoResponse,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettingsResponse,

    @JsonProperty("mail_configuration")
    val mailConfiguration: MailConfigurationResponse,

    @JsonProperty("google_drive_settings")
    val googleDriveSettings: GoogleDriveSettingsResponse,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            company: Company,
            logoStorageService: LogoStorageService,
            googleDriveConfigurationService: GoogleDriveConfigurationService
        ): CompanySettingsResponse = CompanySettingsResponse(
            id = company.id.value,
            companyId = company.id.value,
            basicInfo = CompanyBasicInfoResponse(
                companyName = company.name,
                taxId = company.taxId,
                address = company.address,
                phone = company.phone,
                website = company.website,
                logoId = company.logoId,
                logoUrl = company.logoId?.let { logoStorageService.getLogoUrl(it) }
            ),
            bankSettings = BankSettingsResponse.from(company.bankSettings),
            mailConfiguration = MailConfigurationResponse.from(company.mailConfiguration),
            googleDriveSettings = GoogleDriveSettingsResponse.from(
                company.googleDriveSettings,
                googleDriveConfigurationService
            ),
            createdAt = company.createdAt,
            updatedAt = company.updatedAt
        )
    }
}

data class CompanyBasicInfoResponse(
    @JsonProperty("company_name")
    val companyName: String,

    @JsonProperty("tax_id")
    val taxId: String,

    @JsonProperty("address")
    val address: String?,

    @JsonProperty("phone")
    val phone: String?,

    @JsonProperty("website")
    val website: String?,

    @JsonProperty("logo_id")
    val logoId: String?,

    @JsonProperty("logo_url")
    val logoUrl: String?
)

data class BankSettingsResponse(
    @JsonProperty("bank_account_number")
    val bankAccountNumber: String?,

    @JsonProperty("bank_name")
    val bankName: String?,

    @JsonProperty("swift_code")
    val swiftCode: String?,

    @JsonProperty("account_holder_name")
    val accountHolderName: String?
) {
    companion object {
        fun from(bankSettings: BankSettings): BankSettingsResponse =
            BankSettingsResponse(
                bankAccountNumber = bankSettings.bankAccountNumber,
                bankName = bankSettings.bankName,
                swiftCode = bankSettings.swiftCode,
                accountHolderName = bankSettings.accountHolderName
            )
    }
}

data class MailConfigurationResponse(
    @JsonProperty("smtp_server")
    val smtpServer: String?,

    @JsonProperty("smtp_port")
    val smtpPort: Int?,

    @JsonProperty("email")
    val email: String?,

    @JsonProperty("email_password")
    val emailPassword: String?,

    @JsonProperty("use_tls")
    val useTls: Boolean,

    @JsonProperty("use_ssl")
    val useSsl: Boolean,

    @JsonProperty("from_name")
    val fromName: String?,

    @JsonProperty("enabled")
    val enabled: Boolean
) {
    companion object {
        fun from(mailConfiguration: MailConfiguration): MailConfigurationResponse =
            MailConfigurationResponse(
                smtpServer = mailConfiguration.smtpServer,
                smtpPort = mailConfiguration.smtpPort,
                email = mailConfiguration.email,
                emailPassword = mailConfiguration.emailPassword,
                useTls = mailConfiguration.useTls,
                useSsl = mailConfiguration.useSsl,
                fromName = mailConfiguration.fromName,
                enabled = mailConfiguration.enabled
            )
    }
}

data class GoogleDriveSettingsResponse(
    @JsonProperty("client_id")
    val clientId: String?, // częściowo ukryte ze względów bezpieczeństwa

    // client_secret i refresh_token są całkowicie pomijane w odpowiedzi

    @JsonProperty("default_folder_id")
    val defaultFolderId: String?,

    @JsonProperty("default_folder_name")
    val defaultFolderName: String?,

    @JsonProperty("system_email")
    val systemEmail: String, // email konta systemowego z konfiguracji

    @JsonProperty("enabled")
    val enabled: Boolean,

    @JsonProperty("auto_upload_invoices")
    val autoUploadInvoices: Boolean,

    @JsonProperty("auto_create_folders")
    val autoCreateFolders: Boolean,

    @JsonProperty("configuration_valid")
    val configurationValid: Boolean // czy OAuth credentials są poprawnie skonfigurowane
) {
    companion object {
        fun from(
            googleDriveSettings: GoogleDriveSettings,
            configurationService: GoogleDriveConfigurationService
        ): GoogleDriveSettingsResponse =
            GoogleDriveSettingsResponse(
                clientId = googleDriveSettings.clientId?.let {
                    // Pokazuj tylko pierwsze 10 i ostatnie 4 znaki dla bezpieczeństwa
                    // np. "1234567890...m.com"
                    if (it.length > 14) "${it.take(10)}...${it.takeLast(4)}" else it
                },
                defaultFolderId = googleDriveSettings.defaultFolderId,
                defaultFolderName = googleDriveSettings.defaultFolderName,
                systemEmail = configurationService.getSystemEmail(),
                enabled = googleDriveSettings.enabled,
                autoUploadInvoices = googleDriveSettings.autoUploadInvoices,
                autoCreateFolders = googleDriveSettings.autoCreateFolders,
                configurationValid = configurationService.isConfigurationValid()
            )
    }
}