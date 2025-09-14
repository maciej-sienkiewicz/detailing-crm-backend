package com.carslab.crm.production.modules.media.domain.model

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import java.time.LocalDateTime

data class Media(
    val id: MediaId,
    val companyId: Long,
    val context: MediaContext,
    val entityId: Long?, // nullable dla standalone
    val visitId: VisitId?, // zawsze wypełnione jeśli context = VISIT
    val vehicleId: VehicleId?, // zawsze wypełnione jeśli context = VEHICLE lub pochodzące z wizyty
    val name: String,
    val description: String?,
    val location: String?,
    val tags: List<String>,
    val type: MediaType,
    val size: Long,
    val contentType: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(companyId > 0) { "Company ID must be positive" }
        require(name.isNotBlank()) { "Media name cannot be blank" }
        require(size >= 0) { "Media size cannot be negative" }
        require(contentType.isNotBlank()) { "Content type cannot be blank" }
        require(tags.size <= 20) { "Too many tags (max 20)" }

        // Walidacja kontekstu
        when (context) {
            MediaContext.VISIT -> {
                require(visitId != null) { "Visit ID is required for VISIT context" }
                require(entityId == visitId.value) { "Entity ID must match visit ID for VISIT context" }
            }
            MediaContext.VEHICLE -> {
                require(vehicleId != null) { "Vehicle ID is required for VEHICLE context" }
                require(entityId == vehicleId.value) { "Entity ID must match vehicle ID for VEHICLE context" }
            }
            MediaContext.STANDALONE -> {
                require(entityId == null) { "Entity ID must be null for STANDALONE context" }
            }
        }
    }

    fun isVisitMedia(): Boolean = context.isVisit()
    fun isVehicleMedia(): Boolean = context.isVehicle()
    fun isStandalone(): Boolean = context.isStandalone()

    fun canBeAccessedBy(companyId: Long): Boolean = this.companyId == companyId

    companion object {
        fun createForVisit(
            visitId: VisitId,
            vehicleId: VehicleId,
            companyId: Long,
            name: String,
            description: String?,
            location: String?,
            tags: List<String>,
            type: MediaType,
            size: Long,
            contentType: String,
            storageId: String
        ): Media {
            return Media(
                id = MediaId.of(storageId),
                companyId = companyId,
                context = MediaContext.VISIT,
                entityId = visitId.value,
                visitId = visitId,
                vehicleId = vehicleId,
                name = name,
                description = description,
                location = location,
                tags = tags,
                type = type,
                size = size,
                contentType = contentType,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        fun createForVehicle(
            vehicleId: VehicleId,
            companyId: Long,
            name: String,
            description: String?,
            location: String?,
            tags: List<String>,
            type: MediaType,
            size: Long,
            contentType: String,
            storageId: String
        ): Media {
            return Media(
                id = MediaId.of(storageId),
                companyId = companyId,
                context = MediaContext.VEHICLE,
                entityId = vehicleId.value,
                visitId = null,
                vehicleId = vehicleId,
                name = name,
                description = description,
                location = location,
                tags = tags,
                type = type,
                size = size,
                contentType = contentType,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
}