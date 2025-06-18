// src/main/kotlin/com/carslab/crm/modules/visits/api/dto/ProtocolListDtos.kt
package com.carslab.crm.modules.visits.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Response DTO that matches frontend expectations EXACTLY
data class ProtocolListResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("vehicle")
    val vehicle: VehicleBasicInfo,

    @JsonProperty("period")
    val period: PeriodInfo,

    @JsonProperty("owner") // ← Frontend expects "owner", not "client"
    val owner: OwnerBasicInfo,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("totalServiceCount") // ← camelCase 
    val totalServiceCount: Int,

    @JsonProperty("totalAmount") // ← camelCase
    val totalAmount: Double,

    @JsonProperty("calendarColorId") // ← camelCase
    val calendarColorId: String?,

    @JsonProperty("selectedServices") // ← camelCase
    val selectedServices: List<SelectedServiceResponse>,

    @JsonProperty("lastUpdate") // ← camelCase
    val lastUpdate: String
)

data class VehicleBasicInfo(
    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("licensePlate") // ← camelCase, not license_plate
    val licensePlate: String,

    @JsonProperty("productionYear") // ← Required by frontend
    val productionYear: Int?,

    @JsonProperty("color")
    val color: String?
)

data class PeriodInfo(
    @JsonProperty("startDate")
    val startDate: String,

    @JsonProperty("endDate")
    val endDate: String
)

data class OwnerBasicInfo(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("companyName") // ← camelCase
    val companyName: String?
)

data class SelectedServiceResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("discountType")
    val discountType: String?,

    @JsonProperty("discountValue")
    val discountValue: Double?,

    @JsonProperty("finalPrice")
    val finalPrice: Double,

    @JsonProperty("approvalStatus")
    val approvalStatus: String?,

    @JsonProperty("addedAt")
    val addedAt: String?,

    @JsonProperty("approvedAt")
    val approvedAt: String?,

    @JsonProperty("rejectedAt")
    val rejectedAt: String?,

    @JsonProperty("confirmationMessage")
    val confirmationMessage: String?,

    @JsonProperty("clientMessage")
    val clientMessage: String?,

    @JsonProperty("note")
    val note: String?
)

// Paginated wrapper
data class ProtocolListPaginatedResponse(
    @JsonProperty("data")
    val data: List<ProtocolListResponse>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("size")
    val size: Int,

    @JsonProperty("total_items") // ← Keep snake_case as client handles conversion
    val totalItems: Long,

    @JsonProperty("total_pages") // ← Keep snake_case as client handles conversion
    val totalPages: Int
)

// Alternative: Use camelCase everywhere and configure Jackson globally
data class ProtocolListPaginatedResponseCamelCase(
    @JsonProperty("data")
    val data: List<ProtocolListResponse>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("size")
    val size: Int,

    @JsonProperty("totalItems") // ← camelCase - requires Jackson config
    val totalItems: Long,

    @JsonProperty("totalPages") // ← camelCase - requires Jackson config
    val totalPages: Int
)