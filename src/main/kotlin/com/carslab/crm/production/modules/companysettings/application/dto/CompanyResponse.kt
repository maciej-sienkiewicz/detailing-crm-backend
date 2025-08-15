package com.carslab.crm.production.modules.companysettings.application.dto

import com.carslab.crm.production.modules.companysettings.domain.model.BankSettings
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CompanyResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("basic_info")
    val basicInfo: CompanyBasicInfoResponse,

    @JsonProperty("bank_settings")
    val bankSettings: BankSettingsResponse,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(company: Company): CompanyResponse = CompanyResponse(
            id = company.id.value,
            basicInfo = CompanyBasicInfoResponse(
                companyName = company.name,
                taxId = company.taxId,
                address = company.address,
                phone = company.phone,
                website = company.website
            ),
            bankSettings = BankSettingsResponse.from(company.bankSettings),
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

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(company: Company): CompanySettingsResponse = CompanySettingsResponse(
            id = company.id.value,
            companyId = company.id.value,
            basicInfo = CompanyBasicInfoResponse(
                companyName = company.name,
                taxId = company.taxId,
                address = company.address,
                phone = company.phone,
                website = company.website
            ),
            bankSettings = BankSettingsResponse.from(company.bankSettings),
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
    val website: String?
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