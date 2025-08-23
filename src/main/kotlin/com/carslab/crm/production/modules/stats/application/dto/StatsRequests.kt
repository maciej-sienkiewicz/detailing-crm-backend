package com.carslab.crm.production.modules.stats.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    @field:Size(max = 100, message = "Category name cannot exceed 100 characters")
    val name: String
)

data class AddToCategoryRequest(
    @JsonProperty("service_ids")
    val serviceIds: List<String>
)

data class CategoryStatsRequest(
    @JsonProperty("start_date")
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,
    @JsonProperty("end_date")
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate,
    @field:NotNull(message = "Granularity is required")
    val granularity: TimeGranularity
)

data class ServiceStatsRequest(
    @JsonProperty("service_id")
    @field:NotNull(message = "Service ID is required")
    val serviceId: String,
    @JsonProperty("start_date")
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,
    @JsonProperty("end_date")
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate,
    @field:NotNull(message = "Granularity is required")
    val granularity: TimeGranularity
)

enum class TimeGranularity {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}