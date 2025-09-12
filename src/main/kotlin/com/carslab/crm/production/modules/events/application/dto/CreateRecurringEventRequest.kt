package com.carslab.crm.production.modules.events.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Duration

data class CreateRecurringEventRequest(
    @field:NotBlank(message = "Event title is required")
    @field:Size(max = 200, message = "Event title cannot exceed 200 characters")
    val title: String,

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Event type is required")
    val type: String,

    @field:Valid
    @field:NotNull(message = "Recurrence pattern is required")
    @JsonProperty("recurrence_pattern")
    val recurrencePattern: RecurrencePatternRequest,

    @field:Valid
    @JsonProperty("visit_template")
    val visitTemplate: VisitTemplateRequest? = null
)

data class RecurrencePatternRequest(
    @field:NotNull(message = "Frequency is required")
    val frequency: String,

    @field:Min(value = 1, message = "Interval must be at least 1")
    val interval: Int = 1,

    @JsonProperty("days_of_week")
    val daysOfWeek: List<String>? = null,

    @JsonProperty("day_of_month")
    val dayOfMonth: Int? = null,

    @JsonProperty("end_date")
    val endDate: String? = null,

    @JsonProperty("max_occurrences")
    val maxOccurrences: Int? = null
)

data class VisitTemplateRequest(
    @JsonProperty("client_id")
    val clientId: Long? = null,

    @JsonProperty("vehicle_id")
    val vehicleId: Long? = null,

    @field:NotNull(message = "Estimated duration is required")
    @JsonProperty("estimated_duration_minutes")
    val estimatedDurationMinutes: Long,

    @JsonProperty("default_services")
    val defaultServices: List<ServiceTemplateRequest> = emptyList(),

    val notes: String? = null
)

data class ServiceTemplateRequest(
    @field:NotBlank(message = "Service name is required")
    val name: String,

    @field:NotNull(message = "Base price is required")
    @JsonProperty("base_price")
    val basePrice: java.math.BigDecimal
)
