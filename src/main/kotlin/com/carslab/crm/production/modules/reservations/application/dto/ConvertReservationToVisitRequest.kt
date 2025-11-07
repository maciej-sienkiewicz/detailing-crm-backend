package com.carslab.crm.production.modules.reservations.application.dto

import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request do przekonwertowania rezerwacji na wizytę
 * Tutaj podajemy wszystkie brakujące dane
 */
data class ConvertReservationToVisitRequest(
    @field:NotBlank(message = "Owner name is required")
    @JsonProperty("owner_name")
    val ownerName: String,

    @JsonProperty("owner_id")
    val ownerId: Long? = null,

    @field:Email(message = "Invalid email format")
    @JsonProperty("email")
    val email: String?,

    @JsonProperty("phone")
    val phone: String? = null,

    @JsonProperty("company_name")
    val companyName: String?,

    @JsonProperty("tax_id")
    val taxId: String?,

    @JsonProperty("address")
    val address: String?,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String? = null,

    @JsonProperty("referral_source")
    val referralSource: ApiReferralSource? = null,

    @JsonProperty("other_source_details")
    val otherSourceDetails: String? = null,

    @field:NotBlank(message = "License plate is required")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("production_year")
    val productionYear: Int?,

    @JsonProperty("mileage")
    val mileage: Long?,

    @JsonProperty("vin")
    val vin: String?,

    @JsonProperty("color")
    val color: String?,

    @JsonProperty("selected_services")
    val selectedServices: List<CreateServiceCommand>? = null,

    @JsonProperty("keys_provided")
    val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean? = false,

    @field:Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    @JsonProperty("additional_notes")
    val additionalNotes: String?,

    @JsonProperty("notes")
    val notes: String? = null
)