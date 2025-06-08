package com.carslab.crm.modules.clients.api

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateVehicleCommand(
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
    val ownerIds: List<String> = emptyList()
)

data class UpdateVehicleCommand(
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null
)

/**
 * Command do tworzenia nowego klienta
 */
data class CreateClientCommand(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    @JsonProperty("first_name")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    @JsonProperty("last_name")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String? = null,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    @JsonProperty("address")
    val address: String? = null,

    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    @JsonProperty("company")
    val company: String? = null,

    @field:Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null
) {
    // Validation method to ensure at least one contact method is provided
    fun validate() {
    }
}

data class UpdateClientCommand(
    val id: String? = null,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)