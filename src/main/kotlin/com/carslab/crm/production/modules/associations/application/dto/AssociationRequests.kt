package com.carslab.crm.production.modules.associations.application.dto

import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class CreateAssociationRequest(
    @field:NotNull(message = "Client ID is required")
    @field:Positive(message = "Client ID must be positive")
    @JsonProperty("client_id")
    val clientId: Long,

    @field:NotNull(message = "Vehicle ID is required")
    @field:Positive(message = "Vehicle ID must be positive")
    @JsonProperty("vehicle_id")
    val vehicleId: Long,

    @JsonProperty("association_type")
    val associationType: AssociationType? = AssociationType.OWNER,

    @JsonProperty("is_primary")
    val isPrimary: Boolean? = false
)

data class AssociationResponse(
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("vehicle_id")
    val vehicleId: String,
    @JsonProperty("association_type")
    val associationType: AssociationType,
    @JsonProperty("is_primary")
    val isPrimary: Boolean,
    @JsonProperty("start_date")
    val startDate: LocalDateTime,
    @JsonProperty("end_date")
    val endDate: LocalDateTime?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(association: ClientVehicleAssociation): AssociationResponse {
            return AssociationResponse(
                clientId = association.clientId.value.toString(),
                vehicleId = association.vehicleId.value.toString(),
                associationType = association.associationType,
                isPrimary = association.isPrimary,
                startDate = association.startDate,
                endDate = association.endDate,
                isActive = association.isActive,
                createdAt = association.createdAt
            )
        }
    }
}