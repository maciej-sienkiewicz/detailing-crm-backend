package com.carslab.crm.production.modules.visits.domain.model

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

data class ServiceDiscount(
    val type: DiscountType,
    val value: BigDecimal
) {
    fun applyTo(amount: BigDecimal): BigDecimal {
        return when (type) {
            DiscountType.PERCENTAGE -> amount.multiply(BigDecimal.ONE.subtract(value.divide(BigDecimal.valueOf(100))))
            DiscountType.AMOUNT -> amount.subtract(value)
            DiscountType.FIXED_PRICE -> value
        }
    }
}

enum class DiscountType {
    PERCENTAGE,
    AMOUNT,
    FIXED_PRICE
}

enum class ServiceApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}