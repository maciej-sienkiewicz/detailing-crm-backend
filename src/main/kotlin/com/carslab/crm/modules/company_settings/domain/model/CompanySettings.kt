package com.carslab.crm.modules.company_settings.domain.model

import com.carslab.crm.modules.company_settings.domain.model.shared.AuditInfo
import com.carslab.crm.domain.model.events.CompanySettingsEvent
import com.carslab.crm.modules.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.fasterxml.jackson.annotation.JsonProperty

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
    @JsonProperty("company_name")
    val companyName: String,
    @JsonProperty("tax_id")
    val taxId: String,
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

data class LogoSettings(
    val logoFileId: String? = null,
    val logoFileName: String? = null,
    val logoContentType: String? = null,
    val logoSize: Long? = null,
    val logoUrl: String? = null
) {
    fun hasLogo(): Boolean = !logoFileId.isNullOrBlank()
}

data class CreateCompanySettings(
    val companyId: Long,
    val basicInfo: CompanyBasicInfo,
    val bankSettings: BankSettings = BankSettings(),
    val logoSettings: LogoSettings = LogoSettings(),
    val audit: AuditInfo = AuditInfo()
)