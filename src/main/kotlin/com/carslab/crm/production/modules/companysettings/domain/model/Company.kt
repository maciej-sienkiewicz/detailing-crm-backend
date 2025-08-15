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
    val bankSettings: BankSettings,
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