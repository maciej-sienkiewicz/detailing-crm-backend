package com.carslab.crm.company_settings.api.responses

import com.carslab.crm.company_settings.domain.model.CompanySettings
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CompanySettingsResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfoResponse,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettingsResponse,

    @JsonProperty("email_settings")
    val emailSettings: EmailSettingsResponse,

    @JsonProperty("logo_settings")
    val logoSettings: LogoSettingsResponse,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(settings: CompanySettings): CompanySettingsResponse = CompanySettingsResponse(
            id = settings.id.value,
            companyId = settings.companyId,
            basicInfo = CompanyBasicInfoResponse.from(settings.basicInfo),
            bankSettings = BankSettingsResponse.from(settings.bankSettings),
            emailSettings = EmailSettingsResponse.from(settings.emailSettings),
            logoSettings = LogoSettingsResponse.from(settings.logoSettings),
            createdAt = settings.audit.createdAt,
            updatedAt = settings.audit.updatedAt
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
    val website: String?
) {
    companion object {
        fun from(basicInfo: com.carslab.crm.company_settings.domain.model.CompanyBasicInfo): CompanyBasicInfoResponse =
            CompanyBasicInfoResponse(
                companyName = basicInfo.companyName,
                taxId = basicInfo.taxId,
                address = basicInfo.address,
                phone = basicInfo.phone,
                website = basicInfo.website
            )
    }
}

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
        fun from(bankSettings: com.carslab.crm.company_settings.domain.model.BankSettings): BankSettingsResponse =
            BankSettingsResponse(
                bankAccountNumber = bankSettings.bankAccountNumber,
                bankName = bankSettings.bankName,
                swiftCode = bankSettings.swiftCode,
                accountHolderName = bankSettings.accountHolderName
            )
    }
}

data class EmailSettingsResponse(
    @JsonProperty("smtp_host")
    val smtpHost: String?,

    @JsonProperty("smtp_port")
    val smtpPort: Int?,

    @JsonProperty("smtp_username")
    val smtpUsername: String?,

    @JsonProperty("smtp_password_configured")
    val smtpPasswordConfigured: Boolean,

    @JsonProperty("imap_host")
    val imapHost: String?,

    @JsonProperty("imap_port")
    val imapPort: Int?,

    @JsonProperty("imap_username")
    val imapUsername: String?,

    @JsonProperty("imap_password_configured")
    val imapPasswordConfigured: Boolean,

    @JsonProperty("sender_email")
    val senderEmail: String?,

    @JsonProperty("sender_name")
    val senderName: String?,

    @JsonProperty("use_ssl")
    val useSSL: Boolean,

    @JsonProperty("use_tls")
    val useTLS: Boolean,

    @JsonProperty("smtp_configured")
    val smtpConfigured: Boolean,

    @JsonProperty("imap_configured")
    val imapConfigured: Boolean
) {
    companion object {
        fun from(emailSettings: com.carslab.crm.company_settings.domain.model.EmailSettings): EmailSettingsResponse =
            EmailSettingsResponse(
                smtpHost = emailSettings.smtpHost,
                smtpPort = emailSettings.smtpPort,
                smtpUsername = emailSettings.smtpUsername,
                smtpPasswordConfigured = !emailSettings.smtpPassword.isNullOrBlank(),
                imapHost = emailSettings.imapHost,
                imapPort = emailSettings.imapPort,
                imapUsername = emailSettings.imapUsername,
                imapPasswordConfigured = !emailSettings.imapPassword.isNullOrBlank(),
                senderEmail = emailSettings.senderEmail,
                senderName = emailSettings.senderName,
                useSSL = emailSettings.useSSL,
                useTLS = emailSettings.useTLS,
                smtpConfigured = emailSettings.hasValidSmtpConfig(),
                imapConfigured = emailSettings.hasValidImapConfig()
            )
    }
}

data class LogoSettingsResponse(
    @JsonProperty("has_logo")
    val hasLogo: Boolean,

    @JsonProperty("logo_file_name")
    val logoFileName: String?,

    @JsonProperty("logo_content_type")
    val logoContentType: String?,

    @JsonProperty("logo_size")
    val logoSize: Long?,

    @JsonProperty("logo_url")
    val logoUrl: String?
) {
    companion object {
        fun from(logoSettings: com.carslab.crm.company_settings.domain.model.LogoSettings): LogoSettingsResponse =
            LogoSettingsResponse(
                hasLogo = logoSettings.hasLogo(),
                logoFileName = logoSettings.logoFileName,
                logoContentType = logoSettings.logoContentType,
                logoSize = logoSettings.logoSize,
                logoUrl = logoSettings.logoUrl
            )
    }
}

data class EmailTestResponse(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("error_details")
    val errorDetails: String? = null
)