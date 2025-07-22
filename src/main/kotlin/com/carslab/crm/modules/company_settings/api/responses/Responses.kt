package com.carslab.crm.modules.company_settings.api.responses

import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CompanySettingsResponse(
    @JsonProperty("id")
    val id: Long?,

    @JsonProperty("company_id")
    val companyId: Long?,

    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfoResponse?,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettingsResponse?,

    @JsonProperty("logo_settings")
    val logoSettings: LogoSettingsResponse?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime?,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(settings: CompanySettings): CompanySettingsResponse = CompanySettingsResponse(
            id = settings.id.value,
            companyId = settings.companyId,
            basicInfo = CompanyBasicInfoResponse.from(settings.basicInfo),
            bankSettings = BankSettingsResponse.from(settings.bankSettings),
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
        fun from(basicInfo: com.carslab.crm.modules.company_settings.domain.model.CompanyBasicInfo): CompanyBasicInfoResponse =
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
        fun from(bankSettings: com.carslab.crm.modules.company_settings.domain.model.BankSettings): BankSettingsResponse =
            BankSettingsResponse(
                bankAccountNumber = bankSettings.bankAccountNumber,
                bankName = bankSettings.bankName,
                swiftCode = bankSettings.swiftCode,
                accountHolderName = bankSettings.accountHolderName
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
        fun from(logoSettings: com.carslab.crm.modules.company_settings.domain.model.LogoSettings): LogoSettingsResponse =
            LogoSettingsResponse(
                hasLogo = logoSettings.hasLogo(),
                logoFileName = logoSettings.logoFileName,
                logoContentType = logoSettings.logoContentType,
                logoSize = logoSettings.logoSize,
                logoUrl = logoSettings.logoUrl
            )
    }
}