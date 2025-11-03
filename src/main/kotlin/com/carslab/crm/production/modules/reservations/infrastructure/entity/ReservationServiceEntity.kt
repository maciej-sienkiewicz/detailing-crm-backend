package com.carslab.crm.production.modules.reservations.infrastructure.entity

import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "reservation_services")
class ReservationServiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "reservation_id", nullable = false)
    val reservationId: Long,

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

    @Column(columnDefinition = "TEXT")
    val note: String? = null,

    @Column(name = "final_price_netto", nullable = false, precision = 10, scale = 2)
    val finalPriceNetto: BigDecimal,

    @Column(name = "final_price_brutto", nullable = false, precision = 10, scale = 2)
    val finalPriceBrutto: BigDecimal,

    @Column(name = "final_tax_amount", nullable = false, precision = 10, scale = 2)
    val finalTaxAmount: BigDecimal
) {
    fun toDomain(): ReservationService {
        val basePrice = PriceValueObject(
            priceNetto = basePriceNetto,
            priceBrutto = basePriceBrutto,
            taxAmount = baseTaxAmount
        )

        return ReservationService(
            id = serviceId,
            name = name,
            basePrice = basePrice,
            quantity = quantity,
            note = note
        )
    }

    companion object {
        fun fromDomain(service: ReservationService, reservationId: Long): ReservationServiceEntity {
            val finalPrice = service.calculateFinalPrice()

            return ReservationServiceEntity(
                reservationId = reservationId,
                serviceId = service.id,
                name = service.name,
                basePriceNetto = service.basePrice.priceNetto,
                basePriceBrutto = service.basePrice.priceBrutto,
                baseTaxAmount = service.basePrice.taxAmount,
                quantity = service.quantity,
                note = service.note,
                finalPriceNetto = finalPrice.priceNetto,
                finalPriceBrutto = finalPrice.priceBrutto,
                finalTaxAmount = finalPrice.taxAmount
            )
        }
    }
}