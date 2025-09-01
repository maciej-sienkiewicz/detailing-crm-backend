package com.carslab.crm.production.modules.visits.domain.command

import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import java.math.BigDecimal

data class AddServicesToVisitCommand(
    val visitId: VisitId,
    val companyId: Long,
    val services: List<AddServiceItemCommand>
)

data class AddServiceItemCommand(
    val serviceId: String?,
    val name: String,
    val basePrice: BigDecimal,
    val quantity: Long,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    val finalPrice: BigDecimal? = null,
    val note: String? = null,
    val description: String? = null,
    val vatRate: Int = 23
)