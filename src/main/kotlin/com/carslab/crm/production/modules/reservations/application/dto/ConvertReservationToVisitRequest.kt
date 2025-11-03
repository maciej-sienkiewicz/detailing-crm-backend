package com.carslab.crm.production.modules.reservations.application.dto

import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
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

    @field:Email(message = "Invalid email format")
    @JsonProperty("email")
    val email: String?,

    @JsonProperty("company_name")
    val companyName: String?,

    @JsonProperty("tax_id")
    val taxId: String?,

    @JsonProperty("address")
    val address: String?,

    // Dane pojazdu
    @field:NotBlank(message = "License plate is required")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("production_year")
    val productionYear: Int?,

    @JsonProperty("vin")
    val vin: String?,

    @JsonProperty("color")
    val color: String?,

    @JsonProperty("mileage")
    val mileage: Long?,

    // Opcjonalne usługi
    @JsonProperty("selected_services")
    val selectedServices: List<CreateServiceCommand>? = null,

    // Dodatkowe informacje
    @JsonProperty("keys_provided")
    val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean? = false,

    @field:Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    @JsonProperty("additional_notes")
    val additionalNotes: String?
)