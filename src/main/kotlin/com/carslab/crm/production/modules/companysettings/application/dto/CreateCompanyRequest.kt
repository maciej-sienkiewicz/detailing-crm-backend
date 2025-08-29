package com.carslab.crm.production.modules.companysettings.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class CreateCompanyRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    @JsonProperty("company_name")
    val companyName: String,

    @field:NotBlank(message = "Tax ID is required")
    @field:Size(max = 20, message = "Tax ID cannot exceed 20 characters")
    @JsonProperty("tax_id")
    val taxId: String,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    @JsonProperty("address")
    val address: String,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String? = null,

    @field:Size(max = 255, message = "Website cannot exceed 255 characters")
    @JsonProperty("website")
    val website: String? = null,

    @field:Size(max = 50, message = "Bank account number cannot exceed 50 characters")
    @JsonProperty("bank_account_number")
    val bankAccountNumber: String? = null,

    @field:Size(max = 100, message = "Bank name cannot exceed 100 characters")
    @JsonProperty("bank_name")
    val bankName: String? = null,

    @field:Size(max = 20, message = "SWIFT code cannot exceed 20 characters")
    @JsonProperty("swift_code")
    val swiftCode: String? = null,

    @field:Size(max = 200, message = "Account holder name cannot exceed 200 characters")
    @JsonProperty("account_holder_name")
    val accountHolderName: String? = null
)


/**
 * Request do aktualizacji podstawowych informacji o firmie
 * Nie zawiera tax_id ponieważ nie może być zmieniany
 */
data class UpdateBasicInfoRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    @JsonProperty("company_name")
    val companyName: String,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    @JsonProperty("address")
    val address: String? = null,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String? = null,

    @field:Size(max = 255, message = "Website cannot exceed 255 characters")
    @JsonProperty("website")
    val website: String? = null
)

/**
 * Request do aktualizacji danych bankowych firmy
 */
data class UpdateBankSettingsRequest(
    @field:Size(max = 50, message = "Bank account number cannot exceed 50 characters")
    @JsonProperty("bank_account_number")
    val bankAccountNumber: String? = null,

    @field:Size(max = 100, message = "Bank name cannot exceed 100 characters")
    @JsonProperty("bank_name")
    val bankName: String? = null,

    @field:Size(max = 20, message = "SWIFT code cannot exceed 20 characters")
    @JsonProperty("swift_code")
    val swiftCode: String? = null,

    @field:Size(max = 200, message = "Account holder name cannot exceed 200 characters")
    @JsonProperty("account_holder_name")
    val accountHolderName: String? = null
)

/**
 * Request do aktualizacji konfiguracji mailowej
 */
data class UpdateMailConfigurationRequest(
    @field:Size(max = 255, message = "SMTP server cannot exceed 255 characters")
    @JsonProperty("smtp_server")
    val smtpServer: String? = null,

    @field:Min(value = 1, message = "SMTP port must be greater than 0")
    @field:Max(value = 65535, message = "SMTP port must be less than 65536")
    @JsonProperty("smtp_port")
    val smtpPort: Int? = null,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 255, message = "Email password cannot exceed 255 characters")
    @JsonProperty("email_password")
    val emailPassword: String? = null,

    @JsonProperty("use_tls")
    val useTls: Boolean? = null,

    @JsonProperty("use_ssl")
    val useSsl: Boolean? = null,

    @field:Size(max = 255, message = "From name cannot exceed 255 characters")
    @JsonProperty("from_name")
    val fromName: String? = null,

    @JsonProperty("enabled")
    val enabled: Boolean? = null
)

/**
 * Request do aktualizacji ustawień Google Drive
 * Client credentials są pobierane z properties - użytkownik ustawia tylko folder
 */
data class UpdateGoogleDriveSettingsRequest(
    @field:NotBlank(message = "Folder ID is required")
    @field:Size(max = 500, message = "Folder ID cannot exceed 500 characters")
    @JsonProperty("folder_id")
    val folderId: String,

    @field:NotBlank(message = "Folder name is required")
    @field:Size(max = 255, message = "Folder name cannot exceed 255 characters")
    @JsonProperty("folder_name")
    val folderName: String,

    @JsonProperty("enabled")
    val enabled: Boolean? = null,

    @JsonProperty("auto_upload_invoices")
    val autoUploadInvoices: Boolean? = null,

    @JsonProperty("auto_create_folders")
    val autoCreateFolders: Boolean? = null
)