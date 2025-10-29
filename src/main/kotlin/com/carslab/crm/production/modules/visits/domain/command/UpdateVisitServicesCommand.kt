package com.carslab.crm.production.modules.visits.domain.command

import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal

data class UpdateVisitServicesCommand(
    val visitId: VisitId,
    val companyId: Long,
    val serviceUpdates: List<ServiceUpdateItem>
)

data class ServiceUpdateItem(
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,
    val note: String? = null
)