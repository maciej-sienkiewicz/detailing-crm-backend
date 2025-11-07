package com.carslab.crm.production.modules.reservations.infrastructure.entity

import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.shared.domain.value_objects.DiscountType
import com.carslab.crm.production.shared.domain.value_objects.DiscountValueObject
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Encja JPA dla usługi w rezerwacji.
 *
 * Przechowuje dane cenowe wraz z informacjami o rabacie.
 * Przeliczone wartości są denormalizowane dla wydajności zapytań.
 */
@Entity
@Table(
    name = "reservation_services",
    indexes = [
        Index(name = "idx_reservation_services_reservation_id", columnList = "reservation_id"),
        Index(name = "idx_reservation_services_service_id", columnList = "service_id")
    ]
)
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

    // ============ CENA BAZOWA (przed rabatem) ============
    @Column(name = "base_price_netto", nullable = false, precision = 10, scale = 2)
    val basePriceNetto: BigDecimal,

    @Column(name = "base_price_brutto", nullable = false, precision = 10, scale = 2)
    val basePriceBrutto: BigDecimal,

    @Column(name = "base_tax_amount", nullable = false, precision = 10, scale = 2)
    val baseTaxAmount: BigDecimal,

    @Column(nullable = false)
    val quantity: Long,

    // ============ RABAT ============
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 50)
    val discountType: DiscountType? = null,

    @Column(name = "discount_value", precision = 10, scale = 2)
    val discountValue: BigDecimal? = null,

    @Column(name = "discount_reason", length = 500)
    val discountReason: String? = null,

    // ============ CENA JEDNOSTKOWA (po rabacie, przed mnożeniem przez ilość) ============
    @Column(name = "unit_price_netto", nullable = false, precision = 10, scale = 2)
    val unitPriceNetto: BigDecimal,

    @Column(name = "unit_price_brutto", nullable = false, precision = 10, scale = 2)
    val unitPriceBrutto: BigDecimal,

    @Column(name = "unit_tax_amount", nullable = false, precision = 10, scale = 2)
    val unitTaxAmount: BigDecimal,

    // ============ CENA FINALNA (po rabacie i mnożeniu przez ilość) ============
    @Column(name = "final_price_netto", nullable = false, precision = 10, scale = 2)
    val finalPriceNetto: BigDecimal,

    @Column(name = "final_price_brutto", nullable = false, precision = 10, scale = 2)
    val finalPriceBrutto: BigDecimal,

    @Column(name = "final_tax_amount", nullable = false, precision = 10, scale = 2)
    val finalTaxAmount: BigDecimal,

    @Column(columnDefinition = "TEXT")
    val note: String? = null
) {
    /**
     * Konwertuje encję JPA na model domenowy.
     */
    fun toDomain(): ReservationService {
        val basePrice = PriceValueObject(
            priceNetto = basePriceNetto,
            priceBrutto = basePriceBrutto,
            taxAmount = baseTaxAmount
        )

        // Lokalne zmienne dla smart cast
        val localDiscountType = discountType
        val localDiscountValue = discountValue

        val discount = if (localDiscountType != null && localDiscountValue != null) {
            DiscountValueObject.fromRaw(
                type = localDiscountType,
                value = localDiscountValue,
                reason = discountReason
            )
        } else {
            null
        }

        return ReservationService(
            id = serviceId,
            name = name,
            basePrice = basePrice,
            quantity = quantity,
            discount = discount,
            note = note
        )
    }

    companion object {
        /**
         * Tworzy encję JPA z modelu domenowego.
         *
         * KRYTYCZNE: Wykonuje wszystkie obliczenia przez model domenowy.
         * Encja tylko przechowuje wyniki.
         */
        fun fromDomain(service: ReservationService, reservationId: Long): ReservationServiceEntity {
            val unitPrice = service.calculateUnitPrice()
            val finalPrice = service.calculateFinalPrice()

            return ReservationServiceEntity(
                reservationId = reservationId,
                serviceId = service.id,
                name = service.name,

                // Cena bazowa
                basePriceNetto = service.basePrice.priceNetto,
                basePriceBrutto = service.basePrice.priceBrutto,
                baseTaxAmount = service.basePrice.taxAmount,

                quantity = service.quantity,

                // Rabat
                discountType = service.discount?.type,
                discountValue = service.discount?.value,
                discountReason = service.discount?.reason,

                // Cena jednostkowa (po rabacie)
                unitPriceNetto = unitPrice.priceNetto,
                unitPriceBrutto = unitPrice.priceBrutto,
                unitTaxAmount = unitPrice.taxAmount,

                // Cena finalna
                finalPriceNetto = finalPrice.priceNetto,
                finalPriceBrutto = finalPrice.priceBrutto,
                finalTaxAmount = finalPrice.taxAmount,

                note = service.note
            )
        }
    }
}