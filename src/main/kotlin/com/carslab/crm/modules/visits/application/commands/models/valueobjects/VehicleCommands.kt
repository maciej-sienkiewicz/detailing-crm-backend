// src/main/kotlin/com/carslab/crm/modules/visits/application/commands/models/valueobjects/VehicleCommands.kt
package com.carslab.crm.modules.visits.application.commands.models.valueobjects

import java.time.LocalDateTime

data class CreateVehicleCommand(
    val id: String? = null,
    val brand: String,
    val model: String,
    val licensePlate: String? = null,
    val productionYear: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Long? = null
)

data class UpdateVehicleCommand(
    val id: String,
    val brand: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Long? = null
)

data class CreateClientCommand(
    val id: String? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val taxId: String? = null,
    val address: String? = null,
)

data class UpdateClientCommand(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val taxId: String? = null
)

data class CreatePeriodCommand(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class UpdatePeriodCommand(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class CreateServiceCommand(
    val name: String,
    val basePrice: Double,
    val quantity: Long = 1,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val finalPrice: Double? = null,
    val approvalStatus: String = "PENDING",
    val note: String? = null
)

data class UpdateServiceCommand(
    val id: String,
    val name: String,
    val basePrice: Double,
    val quantity: Long,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val finalPrice: Double? = null,
    val approvalStatus: String = "PENDING",
    val note: String? = null
)

data class OverridenInvoiceServiceItem(
    val name: String,
    val basePrice: Double,
    val quantity: Long,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val finalPrice: Double? = null,
)

data class CreateDocumentsCommand(
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false
)

data class UpdateDocumentsCommand(
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false
)

data class CreateMediaCommand(
    val type: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)