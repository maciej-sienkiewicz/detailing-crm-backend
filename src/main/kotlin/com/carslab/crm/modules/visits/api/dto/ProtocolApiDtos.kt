// src/main/kotlin/com/carslab/crm/modules/visits/api/dto/ProtocolApiDtos.kt
package com.carslab.crm.modules.visits.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email

// Request DTOs - FLAT structure to match your JSON
data class CreateProtocolRequest(
    @JsonProperty("title")
    @field:NotBlank(message = "Title is required")
    val title: String,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("start_date")
    @field:NotBlank(message = "Start date is required")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String? = null,

    // Vehicle fields (flat)
    @JsonProperty("license_plate")
    val licensePlate: String? = null,

    @JsonProperty("make")
    @field:NotBlank(message = "Vehicle make is required")
    val make: String,

    @JsonProperty("model")
    @field:NotBlank(message = "Vehicle model is required")
    val model: String,

    @JsonProperty("production_year")
    val productionYear: Int? = null,

    @JsonProperty("mileage")
    val mileage: Long? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("color")
    val color: String? = null,

    // Client fields (flat)
    @JsonProperty("owner_name")
    @field:NotBlank(message = "Owner name is required")
    val ownerName: String,

    @JsonProperty("company_name")
    val companyName: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("email")
    @field:Email(message = "Invalid email format")
    val email: String? = null,

    @JsonProperty("phone")
    val phone: String? = null,

    // Services field - fix mapping
    @JsonProperty("selected_services")
    val services: List<CreateServiceRequest> = emptyList(),

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("status")
    val status: String? = "SCHEDULED",

    @JsonProperty("referral_source")
    val referralSource: String? = null,

    @JsonProperty("keys_provided")
    val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean? = false,

    @JsonProperty("appointment_id")
    val appointmentId: String? = null
) {
    // Helper properties to create nested objects for mappers
    val vehicle: CreateVehicleRequest
        get() = CreateVehicleRequest(
            make = make,
            model = model,
            licensePlate = licensePlate,
            productionYear = productionYear,
            vin = vin,
            color = color,
            mileage = mileage
        )

    val client: CreateClientRequest
        get() = CreateClientRequest(
            name = ownerName,
            email = email,
            phone = phone,
            companyName = companyName,
            taxId = taxId
        )
}

// Nested request DTOs (for mappers)
data class CreateVehicleRequest(
    val make: String,
    val model: String,
    val licensePlate: String? = null,
    val productionYear: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Long? = null
)

data class CreateClientRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val taxId: String? = null
)

data class CreateServiceRequest(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("name")
    @field:NotBlank(message = "Service name is required")
    val name: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("quantity")
    val quantity: Long = 1,

    @JsonProperty("discount_type")
    val discountType: String? = null,

    @JsonProperty("discount_value")
    val discountValue: Double? = null,

    @JsonProperty("final_price")
    val finalPrice: Double? = null,

    @JsonProperty("approval_status")
    val approvalStatus: String? = "PENDING",

    @JsonProperty("note")
    val note: String? = null
)

// Response DTOs remain the same...
data class ProtocolDetailResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("calendar_color_id") val calendarColorId: String,
    @JsonProperty("vehicle") val vehicle: VehicleResponse,
    @JsonProperty("client") val client: ClientResponse,
    @JsonProperty("period") val period: PeriodResponse,
    @JsonProperty("status") val status: String,
    @JsonProperty("services") val services: List<ServiceResponse>,
    @JsonProperty("notes") val notes: String?,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("updated_at") val updatedAt: String
)

data class ProtocolListResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("vehicle") val vehicle: VehicleBasicResponse,
    @JsonProperty("client") val client: ClientBasicResponse,
    @JsonProperty("status") val status: String,
    @JsonProperty("total_amount") val totalAmount: Double,
    @JsonProperty("last_update") val lastUpdate: String
)

data class VehicleResponse(
    @JsonProperty("make") val make: String,
    @JsonProperty("model") val model: String,
    @JsonProperty("license_plate") val licensePlate: String,
    @JsonProperty("production_year") val productionYear: Int,
    @JsonProperty("color") val color: String?
)

data class VehicleBasicResponse(
    @JsonProperty("make") val make: String,
    @JsonProperty("model") val model: String,
    @JsonProperty("license_plate") val licensePlate: String
)

data class ClientResponse(
    @JsonProperty("name") val name: String,
    @JsonProperty("email") val email: String?,
    @JsonProperty("phone") val phone: String?,
    @JsonProperty("company_name") val companyName: String?
)

data class ClientBasicResponse(
    @JsonProperty("name") val name: String
)

data class PeriodResponse(
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("end_date") val endDate: String
)

data class ServiceResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("price") val price: Double,
    @JsonProperty("quantity") val quantity: Long,
    @JsonProperty("final_price") val finalPrice: Double,
    @JsonProperty("status") val status: String
)

data class ProtocolIdResponse(
    @JsonProperty("id") val id: String
)