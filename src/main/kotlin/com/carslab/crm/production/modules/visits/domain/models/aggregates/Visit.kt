package com.carslab.crm.production.modules.visits.domain.models.aggregates

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.*
import com.carslab.crm.production.modules.visits.domain.models.entities.*
import com.carslab.crm.production.modules.visits.domain.models.enums.*
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal
import java.time.LocalDateTime

data class Visit(
    val id: VisitId?,
    val companyId: Long,
    val title: String,
    val clientId: ClientId,
    val vehicleId: VehicleId,
    val period: VisitPeriod,
    val status: VisitStatus,
    val services: List<VisitService>,
    val documents: VisitDocuments,
    val notes: String?,
    val referralSource: ReferralSource?,
    val appointmentId: String?,
    val calendarColorId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deliveryPerson: DeliveryPerson?

) {
    init {
        require(title.isNotBlank()) { "Visit title cannot be blank" }
        require(companyId > 0) { "Company ID must be positive" }
        require(calendarColorId.isNotBlank()) { "Calendar color ID cannot be blank" }
    }

    /**
     * Calculates the total price of all services in the visit
     * Returns PriceValueObject with summed netto, brutto, and tax amounts
     */
    fun calculateTotalPrice(): PriceValueObject {
        if (services.isEmpty()) {
            return PriceValueObject(
                priceNetto = BigDecimal.ZERO,
                priceBrutto = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO
            )
        }

        return services
            .map { it.calculateFinalPrice() }
            .reduce { acc, price -> acc.add(price) }
    }

    /**
     * Legacy method for backward compatibility - returns brutto amount
     */
    fun totalAmountBrutto(): BigDecimal = calculateTotalPrice().priceBrutto

    /**
     * Gets total netto amount for all services
     */
    fun totalAmountNetto(): BigDecimal = calculateTotalPrice().priceNetto

    /**
     * Gets total tax amount for all services
     */
    fun totalTaxAmount(): BigDecimal = calculateTotalPrice().taxAmount

    fun serviceCount(): Int = services.size

    fun changeStatus(newStatus: VisitStatus): Visit {
        require(status.canTransitionTo(newStatus)) {
            "Cannot transition from $status to $newStatus"
        }
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }

    fun updateServices(newServices: List<VisitService>): Visit {
        return copy(services = newServices, updatedAt = LocalDateTime.now())
    }

    fun addNote(note: String): Visit {
        val updatedNotes = if (notes.isNullOrBlank()) note else "$notes\n$note"
        return copy(notes = updatedNotes, updatedAt = LocalDateTime.now())
    }
}