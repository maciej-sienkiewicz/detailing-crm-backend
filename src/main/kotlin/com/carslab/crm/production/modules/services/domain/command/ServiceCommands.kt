package com.carslab.crm.production.modules.services.domain.command

import java.math.BigDecimal

data class CreateServiceCommand(
    val companyId: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val vatRate: Int
)

data class UpdateServiceCommand(
    val serviceId: String,
    val companyId: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val vatRate: Int
)