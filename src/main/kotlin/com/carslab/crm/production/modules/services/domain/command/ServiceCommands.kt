package com.carslab.crm.production.modules.services.domain.command

import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject

data class CreateServiceCommand(
    val companyId: Long,
    val userId: String,
    val userName: String,
    val name: String,
    val description: String?,
    val price: PriceValueObject,
    val vatRate: Int
)

data class UpdateServiceCommand(
    val serviceId: String,
    val companyId: Long,
    val name: String,
    val description: String?,
    val price: PriceValueObject,
    val vatRate: Int
)