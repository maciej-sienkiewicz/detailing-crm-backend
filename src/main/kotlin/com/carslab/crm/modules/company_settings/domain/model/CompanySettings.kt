package com.carslab.crm.company_settings.domain.model

import com.carslab.crm.company_settings.domain.model.shared.AuditInfo
import com.carslab.crm.domain.model.events.CompanySettingsEvent
import java.time.LocalDateTime

data class CompanySettingsId(val value: Long) {
    companion object {
        fun of(value: Long): CompanySettingsId = CompanySettingsId(value)
    }
}

data class CompanySettings(
    val id: CompanySettingsId,
    val companyId: Long,
    val basicInfo: CompanyBasicInfo,
    val bankSettings: BankSettings,
    val emailSettings: EmailSettings,
    val logoSettings: LogoSettings,
    val audit: AuditInfo = AuditInfo()
) {
    fun apply(event: CompanySettingsEvent): CompanySettings {
        return when (event) {
            is CompanySettingsEvent.BasicInfoUpdated -> copy(
                basicInfo = basicInfo.copy(
                    companyName = event.companyName,
                    taxId = event.taxId,
                    address = event.address,
                    phone = event.phone,
                    website = event.website
                ),
                audit = audit.updated()
            )
            is CompanySettingsEvent.BankSettingsUpdated -> copy(
                bankSettings = bankSettings.copy(
                    bankAccountNumber = event.bankAccountNumber,
                    bankName = event.bankName,
                    swiftCode = event.swiftCode,
                    accountHolderName = event.accountHolderName
                ),
                audit = audit.updated()
            )
            is CompanySettingsEvent.EmailSettingsUpdated -> copy(
                emailSettings = emailSettings.copy(
                    smtpHost = event.smtpHost,
                    smtpPort = event.smtpPort,
                    smtpUsername = event.smtpUsername,
                    smtpPassword = event.smtpPassword,
                    imapHost = event.imapHost,
                    imapPort = event.imapPort,
                    imapUsername = event.imapUsername,
                    imapPassword = event.imapPassword,
                    senderEmail = event.senderEmail,
                    senderName = event.senderName,
                    useSSL = event.useSSL,
                    useTLS = event.useTLS
                ),
                audit = audit.updated()
            )
            is CompanySettingsEvent.LogoUpdated -> copy(
                logoSettings = logoSettings.copy(
                    logoFileId = event.logoFileId,
                    logoFileName = event.logoFileName,
                    logoContentType = event.logoContentType,
                    logoSize = event.logoSize
                ),
                audit = audit.updated()
            )
        }
    }
}

data class CompanyBasicInfo(
    val companyName: String,
    val taxId: String, // NIP
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null
)

data class BankSettings(
    val bankAccountNumber: String? = null,
    val bankName: String? = null,
    val swiftCode: String? = null,
    val accountHolderName: String? = null
)

data class EmailSettings(
    val smtpHost: String? = null,
    val smtpPort: Int? = null,
    val smtpUsername: String? = null,
    val smtpPassword: String? = null, // Będzie zaszyfrowane
    val imapHost: String? = null,
    val imapPort: Int? = null,
    val imapUsername: String? = null,
    val imapPassword: String? = null, // Będzie zaszyfrowane
    val senderEmail: String? = null,
    val senderName: String? = null,
    val useSSL: Boolean = true,
    val useTLS: Boolean = true
) {
    fun hasValidSmtpConfig(): Boolean {
        return !smtpHost.isNullOrBlank() &&
                smtpPort != null &&
                !smtpUsername.isNullOrBlank() &&
                !smtpPassword.isNullOrBlank()
    }

    fun hasValidImapConfig(): Boolean {
        return !imapHost.isNullOrBlank() &&
                imapPort != null &&
                !imapUsername.isNullOrBlank() &&
                !imapPassword.isNullOrBlank()
    }
}

data class LogoSettings(
    val logoFileId: String? = null,
    val logoFileName: String? = null,
    val logoContentType: String? = null,
    val logoSize: Long? = null,
    val logoUrl: String? = null // URL do pobrania logo
) {
    fun hasLogo(): Boolean = !logoFileId.isNullOrBlank()
}

data class CreateCompanySettings(
    val companyId: Long,
    val basicInfo: CompanyBasicInfo,
    val bankSettings: BankSettings = BankSettings(),
    val emailSettings: EmailSettings = EmailSettings(),
    val logoSettings: LogoSettings = LogoSettings(),
    val audit: AuditInfo = AuditInfo()
)