package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "visit_services",
    indexes = [
        Index(name = "idx_visit_services_visit_id", columnList = "visitId"),
        Index(name = "idx_visit_services_service_id", columnList = "serviceId"),
        Index(name = "idx_visit_services_name", columnList = "name")
    ]
)
class VisitServiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val serviceId: String,

    @Column(nullable = false)
    val visitId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val basePrice: BigDecimal,

    @Column(nullable = false)
    val quantity: Long,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val discountType: DiscountType? = null,

    @Column(precision = 10, scale = 2)
    val discountValue: BigDecimal? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val finalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val approvalStatus: ServiceApprovalStatus,

    @Column(length = 500)
    val note: String? = null
) {
    fun toDomain(): VisitService {
        val localDiscountType = discountType
        val localDiscountValue = discountValue

        val discount = if (localDiscountType != null && localDiscountValue != null) {
            ServiceDiscount(localDiscountType, localDiscountValue)
        } else null

        return VisitService(
            id = serviceId, // ← ID oryginalnej usługi
            name = name,
            basePrice = basePrice,
            quantity = quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = approvalStatus,
            note = note
        )
    }

    companion object {
        fun fromDomain(service: VisitService, visitId: Long): VisitServiceEntity {
            return VisitServiceEntity(
                id = null,
                serviceId = service.id,
                visitId = visitId,
                name = service.name,
                basePrice = service.basePrice,
                quantity = service.quantity,
                discountType = service.discount?.type,
                discountValue = service.discount?.value,
                finalPrice = service.finalPrice,
                approvalStatus = service.approvalStatus,
                note = service.note
            )
        }
    }
}