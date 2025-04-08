package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for calendar color
 */
data class CalendarColorResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("color")
    val color: String,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)