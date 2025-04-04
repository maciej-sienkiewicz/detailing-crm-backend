package com.carslab.crm.domain.model.view

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import java.math.BigDecimal

class ServiceRecipeView(
    val id: ServiceRecipeId,
    val name: String,
    val description: String? = null,
    val price: BigDecimal,
    val vatRate: Int,
    val audit: Audit
)