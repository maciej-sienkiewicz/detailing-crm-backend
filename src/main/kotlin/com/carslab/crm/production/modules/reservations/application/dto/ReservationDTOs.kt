package com.carslab.crm.production.modules.reservations.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

// Request do tworzenia rezerwacji
data class CreateReservationRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title cannot exceed 200 characters")
    val title: String,

    @field:NotBlank(message = "Contact phone is required")
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    @JsonProperty("contact_phone")
    val contactPhone: String,

    @field:Size(max = 100, message = "Contact name cannot exceed 100 characters")
    @JsonProperty("contact_name")
    val contactName: String?,

    @field:NotBlank(message = "Vehicle make is required")
    @field:Size(max = 100, message = "Vehicle make cannot exceed 100 characters")
    @JsonProperty("vehicle_make")
    val vehicleMake: String,

    @field:NotBlank(message = "Vehicle model is required")
    @field:Size(max = 100, message = "Vehicle model cannot exceed 100 characters")
    @JsonProperty("vehicle_model")
    val vehicleModel: String,

    @JsonProperty("start_date")
    val startDate: String, // ISO format

    @JsonProperty("end_date")
    val endDate: String?, // ISO format, opcjonalne

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    val notes: String?,

    @field:NotBlank(message = "Calendar color ID is required")
    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @field:Valid
    @JsonProperty("selected_services")
    val selectedServices: List<CreateReservationServiceRequest>? = null
)

// Request do aktualizacji rezerwacji
data class UpdateReservationRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title cannot exceed 200 characters")
    val title: String,

    @field:NotBlank(message = "Contact phone is required")
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    @JsonProperty("contact_phone")
    val contactPhone: String,

    @field:Size(max = 100, message = "Contact name cannot exceed 100 characters")
    @JsonProperty("contact_name")
    val contactName: String?,

    @field:NotBlank(message = "Vehicle make is required")
    @JsonProperty("vehicle_make")
    val vehicleMake: String,

    @field:NotBlank(message = "Vehicle model is required")
    @JsonProperty("vehicle_model")
    val vehicleModel: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String?,

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    val notes: String?,

    @field:NotBlank(message = "Calendar color ID is required")
    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @field:Valid
    @JsonProperty("selected_services")
    val selectedServices: List<CreateReservationServiceRequest>? = null
)

// Response dla rezerwacji
data class ReservationResponse(
    val id: String,
    val title: String,
    @JsonProperty("contact_phone")
    val contactPhone: String,
    @JsonProperty("contact_name")
    val contactName: String?,
    @JsonProperty("vehicle_make")
    val vehicleMake: String,
    @JsonProperty("vehicle_model")
    val vehicleModel: String,
    @JsonProperty("vehicle_display")
    val vehicleDisplay: String,
    @JsonProperty("start_date")
    val startDate: String,
    @JsonProperty("end_date")
    val endDate: String,
    val status: String,
    val notes: String?,
    @JsonProperty("calendar_color_id")
    val calendarColorId: String,
    @JsonProperty("visit_id")
    val visitId: Long?,
    @JsonProperty("can_be_converted")
    val canBeConverted: Boolean,
    @JsonProperty("services")
    val services: List<ReservationServiceResponse>,
    @JsonProperty("service_count")
    val serviceCount: Int,
    @JsonProperty("total_price_netto")
    val totalPriceNetto: BigDecimal,
    @JsonProperty("total_price_brutto")
    val totalPriceBrutto: BigDecimal,
    @JsonProperty("total_tax_amount")
    val totalTaxAmount: BigDecimal,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String
)

// Request do zmiany statusu
data class ChangeReservationStatusRequest(
    @field:NotBlank(message = "Status is required")
    val status: String,

    @field:Size(max = 500, message = "Reason cannot exceed 500 characters")
    val reason: String?
)

// Counters dla statusów
data class ReservationCountersResponse(
    val confirmed: Long,
)