package com.carslab.crm.production.modules.visits.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RecurrencePatternRequest(
    val frequency: String,
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