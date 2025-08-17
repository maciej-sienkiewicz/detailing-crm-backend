package com.carslab.crm.production.modules.visits.application.queries.models

import java.math.BigDecimal

data class GetVisitDetailQuery(
    val visitId: String,
    val companyId: Long
)

data class VisitDetailReadModel(
    val id: String,
    val title: String,
    val calendarColorId: String,
    val vehicle: VehicleDetailReadModel,
    val client: ClientDetailReadModel,
    val period: PeriodReadModel,
    val status: String,
    val services: List<ServiceDetailReadModel>,
    val notes: String?,
    val referralSource: String?,
    val otherSourceDetails: String?,
    val documents: DocumentsReadModel,
    val mediaItems: List<MediaItemReadModel>,
    val audit: AuditReadModel,
    val appointmentId: String?
)

data class VehicleDetailReadModel(
    val id: String?,
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val vin: String?,
    val color: String?,
    val mileage: Long?
)

data class ClientDetailReadModel(
    val id: String?,
    val name: String,
    val email: String?,
    val address: String?,
    val phone: String?,
    val companyName: String?,
    val taxId: String?
)

data class ServiceDetailReadModel(
    val id: String,
    val name: String,
    val basePrice: BigDecimal,
    val quantity: Long,
    val discountType: String?,
    val discountValue: BigDecimal,
    val finalPrice: BigDecimal,
    val approvalStatus: String,
    val note: String?
)

data class DocumentsReadModel(
    val keysProvided: Boolean,
    val documentsProvided: Boolean
)

data class MediaItemReadModel(
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