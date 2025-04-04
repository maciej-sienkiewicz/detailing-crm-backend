package com.carslab.crm.api.model.commands

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Komenda dla tworzenia nowego pojazdu.
 * Nie zawiera opcjonalnych identyfikatorów ani pól auto-uzupełnianych.
 */
data class CreateVehicleCommand(
    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("year")
    val year: Int,

    @JsonProperty("mileage")
    val mileage: Long? = null,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("owner_ids")
    val ownerIds: List<String>
)

/**
 * Komenda dla aktualizacji istniejącego pojazdu.
 * Zawiera identyfikator pojazdu, który jest wymagany do aktualizacji.
 */
data class UpdateVehicleCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("year")
    val year: Int,

    @JsonProperty("mileage")
    val mileage: Long? = null,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("owner_ids")
    val ownerIds: List<String>
)

/**
 * DTO dla odpowiedzi zawierającej pełne informacje o pojeździe.
 */
data class VehicleDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("year")
    val year: Int,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("total_services")
    val totalServices: Int,

    @JsonProperty("last_service_date")
    val lastServiceDate: LocalDateTime? = null,

    @JsonProperty("total_spent")
    val totalSpent: Double,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)

/**
 * DTO do prezentacji statystyk pojazdu.
 */
data class VehicleStatisticsDto(
    @JsonProperty("services_no")
    val servicesNo: Long,

    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal
)

/**
 * DTO dla informacji o właścicielu pojazdu.
 */
data class VehicleOwnerDto(
    @JsonProperty("owner_id")
    val ownerId: Long,

    @JsonProperty("owner_name")
    val ownerName: String
)

/**
 * Komenda dla dodania wpisów historii serwisowej.
 */
data class CreateServiceHistoryCommand(
    @JsonProperty("vehicle_id")
    val vehicleId: String? = null,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("protocol_id")
    val protocolId: String? = null
)

/**
 * Komenda do aktualizacji istniejącego wpisu historii serwisowej.
 */
data class UpdateServiceHistoryCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("vehicle_id")
    val vehicleId: String,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("protocol_id")
    val protocolId: String? = null
)

/**
 * DTO dla wpisu historii serwisowej.
 */
data class ServiceHistoryDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("vehicle_id")
    val vehicleId: String,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String
)