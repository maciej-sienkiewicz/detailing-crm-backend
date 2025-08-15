package com.carslab.crm.production.modules.companysettings.domain.command

data class CreateCompanyCommand(
    val companyName: String,
    val taxId: String,
    val address: String,
    val phone: String? = null,
    val website: String? = null,
    val bankAccountNumber: String? = null,
    val bankName: String? = null,
    val swiftCode: String? = null,
    val accountHolderName: String? = null
)