package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.Discount
import com.carslab.crm.domain.model.DiscountType
import com.carslab.crm.domain.model.Money
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import com.carslab.crm.infrastructure.repository.ServiceId
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "protocol_services")
class ProtocolServiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    var protocol: ProtocolEntity,

    @Column(nullable = false)
    var name: String,

    @Column(name = "base_price", nullable = false)
    var basePrice: BigDecimal,

    @Column(name = "final_price", nullable = false)
    var finalPrice: BigDecimal,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    var approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,

    @Column(nullable = true)
    var note: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = true)
    var discountType: DiscountType? = null,

    @Column(name = "discount_value", nullable = true)
    var discountValue: BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ProtocolServiceView {
        val discount = if (discountType != null && discountValue != null) {
            Discount(
                type = discountType!!,
                value = discountValue!!.toDouble(),
                calculatedAmount = Money(basePrice.toDouble() - finalPrice.toDouble())
            )
        } else null

        return ProtocolServiceView(
            id = ServiceId(id),
            name = name,
            basePrice = Money(basePrice.toDouble()),
            discount = discount,
            finalPrice = Money(finalPrice.toDouble()),
            approvalStatus = approvalStatus,
            note = note,
            quantity = quantity.toLong()
        )
    }

    companion object {
        fun fromDomain(
            domain: ProtocolServiceView,
            protocolEntity: ProtocolEntity
        ): ProtocolServiceEntity {
            val entity = ProtocolServiceEntity(
                id = domain.id.id,
                protocol = protocolEntity,
                name = domain.name,
                basePrice = BigDecimal.valueOf(domain.basePrice.amount),
                finalPrice = BigDecimal.valueOf(domain.finalPrice.amount),
                quantity = domain.quantity.toInt(),
                approvalStatus = domain.approvalStatus,
                note = domain.note
            )

            if (domain.discount != null) {
                entity.discountType = domain.discount.type
                entity.discountValue = BigDecimal.valueOf(domain.discount.value)
            }

            return entity
        }
    }
}