package com.carslab.crm.production.modules.visits.domain.model

import java.math.BigDecimal

data class VisitListService(
    val id: String,
    val name: String,
    val finalPrice: BigDecimal
)