// src/main/kotlin/com/carslab/crm/modules/visits/api/dto/ProtocolApiDtos.kt
package com.carslab.crm.modules.visits.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email

interface ProtocolRequestBase {
    val title: String
    val calendarColorId: String
    val startDate: String
    val endDate: String?
    val licensePlate: String?
    val make: String
    val model: String
    val productionYear: Int?
    val mileage: Long?
    val vin: String?
    val color: String?
    val ownerName: String
    val companyName: String?
    val taxId: String?
    val email: String?
    val phone: String?
    val services: List<CreateServiceRequest>
    val notes: String?
    val status: String?
    val referralSource: String?
    val keysProvided: Boolean?
    val documentsProvided: Boolean?
    val appointmentId: String?
    val vehicleImages: List<CreateVehicleImageCommand>?
}

data class CreateProtocolRequest(
    @JsonProperty("title")
    @field:NotBlank(message = "Title is required")
    override val title: String,

    @JsonProperty("calendar_color_id")
    override val calendarColorId: String,

    @JsonProperty("start_date")
    @field:NotBlank(message = "Start date is required")
    override val startDate: String,

    @JsonProperty("end_date")
    override val endDate: String? = null,

    // Vehicle fields (flat)
    @JsonProperty("license_plate")
    override val licensePlate: String? = null,

    @JsonProperty("make")
    @field:NotBlank(message = "Vehicle make is required")
    override val make: String,

    @JsonProperty("model")
    @field:NotBlank(message = "Vehicle model is required")
    override val model: String,

    @JsonProperty("production_year")
    override val productionYear: Int? = null,

    @JsonProperty("mileage")
    override val mileage: Long? = null,

    @JsonProperty("vin")
    override val vin: String? = null,

    @JsonProperty("color")
    override val color: String? = null,

    // Client fields (flat)
    @JsonProperty("owner_name")
    @field:NotBlank(message = "Owner name is required")
    override val ownerName: String,

    @JsonProperty("company_name")
    override val companyName: String? = null,

    @JsonProperty("tax_id")
    override val taxId: String? = null,

    @JsonProperty("email")
    @field:Email(message = "Invalid email format")
    override val email: String? = null,

    @JsonProperty("phone")
    override val phone: String? = null,

    // Services field
    @JsonProperty("selected_services")
    override val services: List<CreateServiceRequest> = emptyList(),

    @JsonProperty("notes")
    override val notes: String? = null,

    @JsonProperty("status")
    override val status: String? = "SCHEDULED",

    @JsonProperty("referral_source")
    override val referralSource: String? = null,

    @JsonProperty("keys_provided")
    override val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    override val documentsProvided: Boolean? = false,

    @JsonProperty("vehicle_images")
    override val vehicleImages: List<CreateVehicleImageCommand>? = null,

    @JsonProperty("appointment_id")
    override val appointmentId: String? = null
) : ProtocolRequestBase {

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

// Update request - separate data class without inheritance
data class UpdateProtocolRequest(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    @field:NotBlank(message = "Title is required")
    override val title: String,

    @JsonProperty("calendar_color_id")
    override val calendarColorId: String,

    @JsonProperty("start_date")
    @field:NotBlank(message = "Start date is required")
    override val startDate: String,

    @JsonProperty("end_date")
    override val endDate: String? = null,

    @JsonProperty("license_plate")
    override val licensePlate: String? = null,

    @JsonProperty("make")
    @field:NotBlank(message = "Vehicle make is required")
    override val make: String,

    @JsonProperty("model")
    @field:NotBlank(message = "Vehicle model is required")
    override val model: String,

    @JsonProperty("production_year")
    override val productionYear: Int? = null,

    @JsonProperty("mileage")
    override val mileage: Long? = null,

    @JsonProperty("vin")
    override val vin: String? = null,

    @JsonProperty("color")
    override val color: String? = null,

    @JsonProperty("owner_name")
    @field:NotBlank(message = "Owner name is required")
    override val ownerName: String,

    @JsonProperty("company_name")
    override val companyName: String? = null,

    @JsonProperty("tax_id")
    override val taxId: String? = null,

    @JsonProperty("email")
    @field:Email(message = "Invalid email format")
    override val email: String? = null,

    @JsonProperty("phone")
    override val phone: String? = null,

    @JsonProperty("selected_services")
    override val services: List<CreateServiceRequest> = emptyList(),

    @JsonProperty("notes")
    override val notes: String? = null,

    @JsonProperty("status")
    override val status: String? = "SCHEDULED",

    @JsonProperty("referral_source")
    override val referralSource: String? = null,

    @JsonProperty("keys_provided")
    override val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    override val documentsProvided: Boolean? = false,

    @JsonProperty("vehicle_images")
    override val vehicleImages: List<CreateVehicleImageCommand>? = null,

    @JsonProperty("appointment_id")
    override val appointmentId: String? = null
) : ProtocolRequestBase {

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
    val quantity: Long = 1, // DODANA DOMYŚLNA WARTOŚĆ!

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

data class CreateVehicleImageCommand(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("size")
    val size: Long? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null,

    @JsonProperty("has_file")
    val hasFile: Boolean = false,

    @JsonProperty("tags")
    val tags: List<String> = emptyList()
)
