package com.carslab.crm.modules.company_settings.api.requests

import com.carslab.crm.modules.company_settings.domain.model.BankSettings
import com.carslab.crm.modules.company_settings.domain.model.CompanyBasicInfo
import com.carslab.crm.modules.company_settings.domain.model.EmailSettings
import com.carslab.crm.modules.company_settings.domain.model.LogoSettings
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCompanySettingsRequest(
    @JsonProperty("company_id")
    val companyId: Long? = null, // Will be set from security context

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
    val address: String? = null,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String? = null,

    @field:Size(max = 255, message = "Website cannot exceed 255 characters")
    @JsonProperty("website")
    val website: String? = null,

    // Bank settings
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
    val accountHolderName: String? = null,

    // Email settings
    @field:Size(max = 255, message = "SMTP host cannot exceed 255 characters")
    @JsonProperty("smtp_host")
    val smtpHost: String? = null,

    @JsonProperty("smtp_port")
    val smtpPort: Int? = null,

    @field:Size(max = 255, message = "SMTP username cannot exceed 255 characters")
    @JsonProperty("smtp_username")
    val smtpUsername: String? = null,

    @field:Size(max = 255, message = "SMTP password cannot exceed 255 characters")
    @JsonProperty("smtp_password")
    val smtpPassword: String? = null,

    @field:Size(max = 255, message = "IMAP host cannot exceed 255 characters")
    @JsonProperty("imap_host")
    val imapHost: String? = null,

    @JsonProperty("imap_port")
    val imapPort: Int? = null,

    @field:Size(max = 255, message = "IMAP username cannot exceed 255 characters")
    @JsonProperty("imap_username")
    val imapUsername: String? = null,

    @field:Size(max = 255, message = "IMAP password cannot exceed 255 characters")
    @JsonProperty("imap_password")
    val imapPassword: String? = null,

    @field:Email(message = "Sender email must be valid")
    @field:Size(max = 255, message = "Sender email cannot exceed 255 characters")
    @JsonProperty("sender_email")
    val senderEmail: String? = null,

    @field:Size(max = 200, message = "Sender name cannot exceed 200 characters")
    @JsonProperty("sender_name")
    val senderName: String? = null,

    @JsonProperty("use_ssl")
    val useSSL: Boolean? = true,

    @JsonProperty("use_tls")
    val useTLS: Boolean? = true
)

data class UpdateCompanySettingsRequest(
    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfo,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettings,

    @JsonProperty("email_settings")
    val emailSettings: EmailSettings,

    @JsonProperty("logo_settings")
    val logoSettings: LogoSettings = LogoSettings()
)

data class TestEmailConnectionRequest(
    @JsonProperty("smtp_host")
    val smtpHost: String,

    @JsonProperty("smtp_port")
    val smtpPort: Int,

    @JsonProperty("smtp_username")
    val smtpUsername: String,

    @JsonProperty("smtp_password")
    val smtpPassword: String,

    @JsonProperty("use_ssl")
    val useSSL: Boolean = true,

    @JsonProperty("use_tls")
    val useTLS: Boolean = true,

    @JsonProperty("test_email")
    val testEmail: String
)