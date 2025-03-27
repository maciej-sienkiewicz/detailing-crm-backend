package com.carslab.crm.api.model.response

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.request.ApiReferralSource
import com.carslab.crm.api.model.request.ServiceApprovalStatus
import com.carslab.crm.domain.model.DiscountType

/**
 * DTO dla odpowiedzi API po utworzeniu lub aktualizacji protokołu przyjęcia pojazdu.
 * Zawiera podstawowe informacje zwrotne.
 */
data class CarReceptionProtocolResponse(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val statusUpdatedAt: String,
    val status: ApiProtocolStatus
)

/**
 * Rozszerzona wersja odpowiedzi zawierająca pełne dane protokołu.
 * Może być używana w endpointach pobierających szczegóły protokołu.
 */
data class CarReceptionProtocolDetailResponse(
    val id: String,
    val startDate: String,
    val endDate: String,
    val licensePlate: String,
    val make: String,
    val model: String,
    val productionYear: Int,
    val mileage: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val keysProvided: Boolean,
    val documentsProvided: Boolean,
    val ownerId: Long? = null,
    val ownerName: String,
    val companyName: String? = null,
    val taxId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
    val selectedServices: List<SelectedServiceResponse>,
    val status: ApiProtocolStatus,
    val referralSource: ApiReferralSource? = null,
    val otherSourceDetails: String? = null,
    val vehicleImages: List<VehicleImageResponse>? = null,
    val createdAt: String,
    val updatedAt: String,
    val statusUpdatedAt: String,
    val appointmentId: String? = null
)

/**
 * DTO dla informacji o usłudze w odpowiedzi.
 */
data class SelectedServiceResponse(
    val id: String,
    val name: String,
    val price: Double,
    val discountType: DiscountType,
    val discountValue: Double,
    val finalPrice: Double,
    val approvalStatus: ServiceApprovalStatus? = null
)

/**
 * DTO dla informacji o zdjęciu w odpowiedzi.
 */
data class VehicleImageResponse(
    val id: String,
    val url: String? = null,
    val name: String,
    val size: Long,
    val type: String,
    val description: String? = null,
    val location: String? = null,
    val createdAt: String
)