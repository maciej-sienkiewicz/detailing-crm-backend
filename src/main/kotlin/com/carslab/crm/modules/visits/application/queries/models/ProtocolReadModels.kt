// src/main/kotlin/com/carslab/crm/modules/visits/application/queries/models/ProtocolReadModels.kt
package com.carslab.crm.modules.visits.application.queries.models

data class ProtocolDetailReadModel(
    val id: String,
    val title: String,
    val calendarColorId: String,
    val vehicle: VehicleReadModel,
    val client: ClientReadModel,
    val period: PeriodReadModel,
    val status: String,
    val services: List<ServiceReadModel>,
    val notes: String?,
    val referralSource: String?,
    val otherSourceDetails: String?,
    val documents: DocumentsReadModel,
    val mediaItems: List<ProtocolMediaReadModel>,
    val audit: AuditReadModel,
    val appointmentId: String?
)

data class ProtocolListReadModel(
    val id: String,
    val title: String,
    val vehicle: VehicleBasicReadModel,
    val client: ClientBasicReadModel,
    val period: PeriodReadModel,
    val status: String,
    val calendarColorId: String,
    val totalServiceCount: Int,
    val totalAmount: Double,
    val lastUpdate: String
)

data class ProtocolCountersReadModel(
    val scheduled: Int,
    val inProgress: Int,
    val readyForPickup: Int,
    val completed: Int,
    val cancelled: Int,
    val all: Int
)

data class VehicleReadModel(
    val id: String?,
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val vin: String?,
    val color: String?,
    val mileage: Long?
)

data class VehicleBasicReadModel(
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val color: String?
)

data class ClientReadModel(
    val id: String?,
    val name: String,
    val email: String?,
    val phone: String?,
    val companyName: String?,
    val taxId: String?
)

data class ClientBasicReadModel(
    val name: String,
    val companyName: String?
)

data class PeriodReadModel(
    val startDate: String,
    val endDate: String
)

data class ServiceReadModel(
    val id: String,
    val name: String,
    val basePrice: Double,
    val quantity: Long,
    val discountType: String?,
    val discountValue: Double,
    val finalPrice: Double,
    val approvalStatus: String,
    val note: String?
)

data class DocumentsReadModel(
    val keysProvided: Boolean,
    val documentsProvided: Boolean
)

data class ProtocolMediaReadModel(
    val id: String,
    val type: String,
    val name: String,
    val size: Long,
    val description: String?,
    val location: String?,
    val tags: List<String>
)

data class AuditReadModel(
    val createdAt: String,
    val updatedAt: String,
    val statusUpdatedAt: String
)