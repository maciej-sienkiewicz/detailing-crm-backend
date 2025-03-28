package com.carslab.crm.api.model.request

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.response.VehicleImageResponse
import com.carslab.crm.domain.model.VehicleImage
import java.util.UUID

/**
 * DTO dla żądania utworzenia lub aktualizacji protokołu przyjęcia pojazdu.
 * Wszystkie pola są opcjonalne, aby umożliwić częściowe aktualizacje.
 */
class CarReceptionProtocolRequest {
    var id: String? = null
    var startDate: String? = null
    var endDate: String? = null
    var licensePlate: String? = null
    var make: String? = null
    var model: String? = null
    var productionYear: Int? = null
    var mileage: Int? = null
    var vin: String? = null
    var color: String? = null
    var keysProvided: Boolean? = null
    var documentsProvided: Boolean? = null
    var ownerId: Long? = null
    var ownerName: String? = null
    var companyName: String? = null
    var taxId: String? = null
    var email: String? = null
    var phone: String? = null
    var notes: String? = null
    var selectedServices: List<SelectedServiceRequest>? = null
    var status: ApiProtocolStatus = ApiProtocolStatus.SCHEDULED
    var appointmentId: String? = null
    var referralSource: ApiReferralSource? = null
    var otherSourceDetails: String? = null
    var vehicleImages: List<VehicleImageRequest>? = null
    var statusUpdatedAt: String? = null

    // Konstruktor domyślny wymagany przez Jackson
    constructor() {}
}

/**
 * DTO dla informacji o usłudze w żądaniu.
 */
class SelectedServiceRequest {
    var id: String? = null
    var name: String? = null
    var price: Double? = null
    var discountType: ApiDiscountType? = null
    var discountValue: Double? = null
    var finalPrice: Double? = null
    val approvalStatus: ServiceApprovalStatus? = null

    // Konstruktor domyślny wymagany przez Jackson
    constructor() {}
}

/**
 * DTO dla informacji o zdjęciu w żądaniu.
 */
class VehicleImageRequest {
    var id: String? = null
    var name: String? = null
    var size: Long? = null
    var type: String? = null
    var description: String? = null
    var location: String? = null
    var has_file: Boolean = false

    // Konstruktor domyślny wymagany przez Jackson
    constructor() {}
}

object VehicleImageMapper {
    fun toResponse(image: VehicleImage): VehicleImageResponse {
        return VehicleImageResponse(
            id = image.id,
            name = image.name,
            size = image.size,
            type = image.type,
            storage_id = image.storageId,
            created_at = image.createdAt,
            description = image.description,
            location = image.location
        )
    }

    fun toDomain(request: VehicleImageRequest, storageId: String? = null): VehicleImage {
        return VehicleImage(
            id = request.id ?: UUID.randomUUID().toString(),
            name = request.name ?: "",
            size = request.size ?: 0,
            type = request.type ?: "",
            storageId = storageId ?: "",  // ID przechowywania w pamięci zostanie ustawione po zapisaniu pliku
            description = request.description,
            location = request.location
        )
    }
}

/**
 * Typ wyliczeniowy dla statusu protokołu.
 */
enum class ProtocolStatus {
    SCHEDULED,
    PENDING_APPROVAL,
    IN_PROGRESS,
    READY_FOR_PICKUP,
    COMPLETED
}

/**
 * Typ wyliczeniowy dla typu rabatu.
 */
enum class ApiDiscountType {
    PERCENTAGE,
    AMOUNT,
    FIXED_PRICE
}

/**
 * Typ wyliczeniowy dla statusu zatwierdzenia usługi.
 */
enum class ServiceApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Typ wyliczeniowy dla źródła polecenia.
 */
enum class ApiReferralSource {
    REGULAR_CUSTOMER,
    RECOMMENDATION,
    SEARCH_ENGINE,
    SOCIAL_MEDIA,
    LOCAL_AD,
    OTHER
}