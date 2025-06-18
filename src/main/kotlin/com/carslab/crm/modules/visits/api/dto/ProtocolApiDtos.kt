package com.carslab.crm.modules.visits.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

// Request DTOs
data class CreateProtocolRequest(
    @JsonProperty("title") val title: String,
    @JsonProperty("calendar_color_id") val calendarColorId: String,
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("end_date") val endDate: String? = null,
    @JsonProperty("vehicle") val vehicle: CreateVehicleRequest,
    @JsonProperty("client") val client: CreateClientRequest,
    @JsonProperty("services") val services: List<CreateServiceRequest> = emptyList(),
    @JsonProperty("notes") val notes: String? = null,
    @JsonProperty("status") val status: String? = "SCHEDULED",
    @JsonProperty("referral_source") val referralSource: String? = null,
    @JsonProperty("keys_provided") val keysProvided: Boolean? = false,
    @JsonProperty("documents_provided") val documentsProvided: Boolean? = false,
    @JsonProperty("appointment_id") val appointmentId: String? = null
)

data class CreateVehicleRequest(
    @JsonProperty("make") val make: String,
    @JsonProperty("model") val model: String,
    @JsonProperty("license_plate") val licensePlate: String? = null,
    @JsonProperty("production_year") val productionYear: Int? = null,
    @JsonProperty("vin") val vin: String? = null,
    @JsonProperty("color") val color: String? = null,
    @JsonProperty("mileage") val mileage: Long? = null
)

data class CreateClientRequest(
    @JsonProperty("name") val name: String,
    @JsonProperty("email") val email: String? = null,
    @JsonProperty("phone") val phone: String? = null,
    @JsonProperty("company_name") val companyName: String? = null,
    @JsonProperty("tax_id") val taxId: String? = null
)

data class CreateServiceRequest(
    @JsonProperty("name") val name: String,
    @JsonProperty("price") val price: Double,
    @JsonProperty("quantity") val quantity: Long,
    @JsonProperty("discount_type") val discountType: String? = null,
    @JsonProperty("discount_value") val discountValue: Double? = null,
    @JsonProperty("note") val note: String? = null
)

// Response DTOs
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