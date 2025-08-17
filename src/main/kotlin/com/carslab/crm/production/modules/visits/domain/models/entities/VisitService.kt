package com.carslab.crm.production.modules.visits.domain.models.entities

import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import java.math.BigDecimal

data class VisitService(
    val id: String,
    val name: String,
    val basePrice: BigDecimal,
    val quantity: Long,
    val discount: ServiceDiscount?,
    val finalPrice: BigDecimal,
    val approvalStatus: ServiceApprovalStatus,
    val note: String?
) {
    fun calculateFinalPrice(): BigDecimal {
        return discount?.applyTo(basePrice.multiply(BigDecimal.valueOf(quantity)))
            ?: basePrice.multiply(BigDecimal.valueOf(quantity))
    }

    fun isApproved(): Boolean = approvalStatus == ServiceApprovalStatus.APPROVED
    fun isPending(): Boolean = approvalStatus == ServiceApprovalStatus.PENDING
    fun isRejected(): Boolean = approvalStatus == ServiceApprovalStatus.REJECTED
}