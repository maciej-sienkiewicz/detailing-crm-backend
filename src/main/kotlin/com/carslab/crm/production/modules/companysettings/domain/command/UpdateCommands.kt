package com.carslab.crm.production.modules.companysettings.domain.command

/**
 * Komenda do aktualizacji podstawowych informacji o firmie
 */
data class UpdateBasicInfoCommand(
    val companyId: Long,
    val companyName: String,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null
)

/**
 * Komenda do aktualizacji danych bankowych
 */
data class UpdateBankSettingsCommand(
    val companyId: Long,
    val bankAccountNumber: String? = null,
    val bankName: String? = null,
    val swiftCode: String? = null,
    val accountHolderName: String? = null
)

/**
 * Komenda do aktualizacji konfiguracji mailowej
 */
data class UpdateMailConfigurationCommand(
    val companyId: Long,
    val smtpServer: String? = null,
    val smtpPort: Int? = null,
    val email: String? = null,
    val emailPassword: String? = null,
    val useTls: Boolean? = null,
    val useSsl: Boolean? = null,
    val fromName: String? = null,
    val enabled: Boolean? = null
)

/**
 * Komenda do aktualizacji ustawień Google Drive
 * Client credentials są pobierane z konfiguracji - użytkownik ustawia tylko folder
 */
data class UpdateGoogleDriveSettingsCommand(
    val companyId: Long,
    val folderId: String,
    val folderName: String,
    val enabled: Boolean? = null,
    val autoUploadInvoices: Boolean? = null,
    val autoCreateFolders: Boolean? = null
)