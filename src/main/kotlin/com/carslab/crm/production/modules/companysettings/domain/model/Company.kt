package com.carslab.crm.production.modules.companysettings.domain.model

import java.time.LocalDateTime

@JvmInline
value class CompanyId(val value: Long) {
    companion object {
        fun of(value: Long): CompanyId = CompanyId(value)
    }
}

data class Company(
    val id: CompanyId,
    val name: String,
    val taxId: String,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val logoId: String? = null,
    val bankSettings: BankSettings,
    val mailConfiguration: MailConfiguration,
    val googleDriveSettings: GoogleDriveSettings,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long
)

data class BankSettings(
    val bankAccountNumber: String? = null,
    val bankName: String? = null,
    val swiftCode: String? = null,
    val accountHolderName: String? = null
)

data class MailConfiguration(
    val smtpServer: String? = null,
    val smtpPort: Int? = null,
    val email: String? = null,
    val emailPassword: String? = null,
    val useTls: Boolean = false,
    val useSsl: Boolean = false,
    val fromName: String? = null,
    val enabled: Boolean = false
)

data class GoogleDriveSettings(
    val clientId: String? = null,
    val clientSecret: String? = null,
    val refreshToken: String? = null,
    val defaultFolderId: String? = null,
    val defaultFolderName: String? = null,
    val enabled: Boolean = false,
    val autoUploadInvoices: Boolean = false,
    val autoCreateFolders: Boolean = false
)