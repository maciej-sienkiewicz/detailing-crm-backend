package com.carslab.crm.api.model.response

import com.carslab.crm.api.model.request.ProtocolStatus

/**
 * DTO dla odpowiedzi API zawierającej tylko podstawowe informacje o protokole,
 * potrzebne do wyświetlenia w widoku listy.
 */
data class CarReceptionProtocolListResponse(
    val id: String,
    val vehicle: VehicleBasicInfo,
    val period: PeriodInfo,
    val owner: OwnerBasicInfo,
    val status: ProtocolStatus,
    val totalServiceCount: Int,
    val totalAmount: Double
)

/**
 * Podstawowe informacje o pojeździe dla widoku listy.
 */
data class VehicleBasicInfo(
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val color: String? = null
)

/**
 * Podstawowe informacje o właścicielu dla widoku listy.
 */
data class OwnerBasicInfo(
    val name: String,
    val companyName: String? = null
)

/**
 * Informacje o okresie usługi dla widoku listy.
 */
data class PeriodInfo(
    val startDate: String,
    val endDate: String
)