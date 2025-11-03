package com.carslab.crm.production.modules.reservations.domain.models.aggregates

import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationPeriod
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.VehicleBasicInfo
import java.time.LocalDateTime

/**
 * Rezerwacja - minimalne dane przed wizytą
 * Nie wymaga pełnych danych klienta ani pojazdu
 */
data class Reservation(
    val id: ReservationId?,
    val companyId: Long,
    val title: String,

    // Minimalne dane kontaktowe
    val contactPhone: String,
    val contactName: String?, // opcjonalne imię

    // Podstawowe info o pojeździe (bez tablicy rejestracyjnej!)
    val vehicleInfo: VehicleBasicInfo,

    val period: ReservationPeriod,
    val status: ReservationStatus,

    // Usługi zaplanowane w rezerwacji
    val services: List<ReservationService>,

    val notes: String?,
    val calendarColorId: String,

    // ID powiązanej wizyty (po konwersji)
    val visitId: Long?,

    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(title.isNotBlank()) { "Reservation title cannot be blank" }
        require(contactPhone.isNotBlank()) { "Contact phone cannot be blank" }
        require(companyId > 0) { "Company ID must be positive" }
        require(calendarColorId.isNotBlank()) { "Calendar color ID cannot be blank" }
    }

    fun canBeConverted(): Boolean {
        return status == ReservationStatus.CONFIRMED && visitId == null
    }

    fun markAsConverted(visitId: Long): Reservation {
        require(canBeConverted()) { "Reservation cannot be converted in current state" }
        return copy(
            visitId = visitId,
            status = ReservationStatus.CONVERTED,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateStatus(newStatus: ReservationStatus): Reservation {
        require(status.canTransitionTo(newStatus)) {
            "Cannot transition from $status to $newStatus"
        }
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }

    fun update(
        title: String,
        contactPhone: String,
        contactName: String?,
        vehicleInfo: VehicleBasicInfo,
        period: ReservationPeriod,
        services: List<ReservationService>,
        notes: String?,
        calendarColorId: String
    ): Reservation {
        require(status != ReservationStatus.CONVERTED) {
            "Cannot update converted reservation"
        }
        require(status != ReservationStatus.CANCELLED) {
            "Cannot update cancelled reservation"
        }

        return copy(
            title = title.trim(),
            contactPhone = contactPhone.trim(),
            contactName = contactName?.trim(),
            vehicleInfo = vehicleInfo,
            period = period,
            services = services,
            notes = notes?.trim(),
            calendarColorId = calendarColorId,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateServices(newServices: List<ReservationService>): Reservation {
        require(status != ReservationStatus.CONVERTED) {
            "Cannot update services for converted reservation"
        }
        require(status != ReservationStatus.CANCELLED) {
            "Cannot update services for cancelled reservation"
        }
        return copy(services = newServices, updatedAt = LocalDateTime.now())
    }

    fun serviceCount(): Int = services.size
}