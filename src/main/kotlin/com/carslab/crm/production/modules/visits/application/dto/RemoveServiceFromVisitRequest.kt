package com.carslab.crm.production.modules.visits.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class RemoveServiceFromVisitRequest(
    @field:NotBlank(message = "Service ID is required")
    @JsonProperty("service_id")
    val serviceId: String,

    val reason: String? = null
)