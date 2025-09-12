package com.carslab.crm.production.modules.events.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class UpdateRecurringEventRequest(
    @field:Size(max = 200, message = "Event title cannot exceed 200 characters")
    val title: String? = null,

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String? = null,

    @field:Valid
    @JsonProperty("recurrence_pattern")
    val recurrencePattern: RecurrencePatternRequest? = null,

    @field:Valid
    @JsonProperty("visit_template")
    val visitTemplate: VisitTemplateRequest? = null
)
