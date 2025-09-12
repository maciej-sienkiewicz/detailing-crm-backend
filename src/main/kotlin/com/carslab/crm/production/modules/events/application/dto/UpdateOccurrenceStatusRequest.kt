package com.carslab.crm.production.modules.events.application.dto

import jakarta.validation.constraints.NotNull

data class UpdateOccurrenceStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: String,

    val notes: String? = null
)