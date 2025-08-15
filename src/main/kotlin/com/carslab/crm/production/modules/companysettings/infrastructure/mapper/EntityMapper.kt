package com.carslab.crm.production.modules.companysettings.infrastructure.mapper

import com.carslab.crm.production.modules.companysettings.domain.model.BankSettings
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.companysettings.infrastructure.entity.CompanyEntity

fun Company.toEntity(): CompanyEntity {
    return CompanyEntity(
        id = if (this.id.value == 0L) null else this.id.value,
        name = this.name,
        taxId = this.taxId,
        address = this.address,
        phone = this.phone,
        website = this.website,
        bankAccountNumber = this.bankSettings.bankAccountNumber,
        bankName = this.bankSettings.bankName,
        swiftCode = this.bankSettings.swiftCode,
        accountHolderName = this.bankSettings.accountHolderName,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}

fun CompanyEntity.toDomain(): Company {
    return Company(
        id = CompanyId.Companion.of(this.id!!),
        name = this.name,
        taxId = this.taxId,
        address = this.address,
        phone = this.phone,
        website = this.website,
        bankSettings = BankSettings(
            bankAccountNumber = this.bankAccountNumber,
            bankName = this.bankName,
            swiftCode = this.swiftCode,
            accountHolderName = this.accountHolderName
        ),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}