package com.carslab.crm.production.modules.visits.domain.models.aggregates

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.*
import com.carslab.crm.production.modules.visits.domain.models.entities.*
import com.carslab.crm.production.modules.visits.domain.models.enums.*
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
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
    val updatedAt: LocalDateTime
) {
    init {
        require(title.isNotBlank()) { "Visit title cannot be blank" }
        require(companyId > 0) { "Company ID must be positive" }
        require(calendarColorId.isNotBlank()) { "Calendar color ID cannot be blank" }
    }

    fun totalAmount(): BigDecimal = services.sumOf { it.finalPrice }
    fun serviceCount(): Int = services.size
    fun hasApprovedServices(): Boolean = services.any { it.isApproved() }
    fun hasPendingServices(): Boolean = services.any { it.isPending() }

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