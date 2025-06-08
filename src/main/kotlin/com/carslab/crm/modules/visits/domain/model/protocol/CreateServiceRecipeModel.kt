package com.carslab.crm.domain.model.create.protocol

import com.carslab.crm.domain.model.Audit
import java.math.BigDecimal

data class ServiceRecipeId(val value: String) {
    override fun toString(): String = value
}

data class CreateServiceRecipeModel(
    val name: String,
    val description: String? = null,
    val price: BigDecimal,
    val vatRate: Int,
    val audit: Audit
)