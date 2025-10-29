package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "visit_services")
class VisitServiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "visit_id", nullable = false)
    val visitId: Long,

    @Column(name = "service_id", nullable = false, length = 36)
    val serviceId: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(name = "base_price_netto", nullable = false, precision = 10, scale = 2)
    val basePriceNetto: BigDecimal,

    @Column(name = "base_price_brutto", nullable = false, precision = 10, scale = 2)
    val basePriceBrutto: BigDecimal,

    @Column(name = "base_tax_amount", nullable = false, precision = 10, scale = 2)
    val baseTaxAmount: BigDecimal,

    @Column(nullable = false)
    val quantity: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    val discountType: DiscountType? = null,

    @Column(name = "discount_value", precision = 10, scale = 2)
    val discountValue: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,

    @Column(length = 500)
    val note: String? = null
) {
    fun toDomain(): VisitService {
        val basePrice = PriceValueObject(
            priceNetto = basePriceNetto,
            priceBrutto = basePriceBrutto,
            taxAmount = baseTaxAmount
        )

        val discount = if (discountType != null && discountValue != null) {
            ServiceDiscount(discountType!!, discountValue!!)
        } else null

        return VisitService(
            id = serviceId,
            name = name,
            basePrice = basePrice,
            quantity = quantity,
            discount = discount,
            approvalStatus = approvalStatus,
            note = note
        )
    }

    companion object {
        fun fromDomain(visitService: VisitService, visitId: Long): VisitServiceEntity {
            return VisitServiceEntity(
                visitId = visitId,
                serviceId = visitService.id,
                name = visitService.name,
                basePriceNetto = visitService.basePrice.priceNetto,
                basePriceBrutto = visitService.basePrice.priceBrutto,
                baseTaxAmount = visitService.basePrice.taxAmount,
                quantity = visitService.quantity,
                discountType = visitService.discount?.type,
                discountValue = visitService.discount?.value,
                approvalStatus = visitService.approvalStatus,
                note = visitService.note
            )
        }
    }
}