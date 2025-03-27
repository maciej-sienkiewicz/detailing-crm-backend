package com.carslab.crm.api.model.request

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
    var status: ProtocolStatus = ProtocolStatus.SCHEDULED
    var appointmentId: String? = null
    var referralSource: ReferralSource? = null
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
    var discountType: DiscountType? = null
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
    var url: String? = null
    var name: String? = null
    var size: Long? = null
    var type: String? = null
    var description: String? = null
    var location: String? = null

    // Konstruktor domyślny wymagany przez Jackson
    constructor() {}
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
enum class DiscountType {
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
enum class ReferralSource {
    REGULAR_CUSTOMER,
    RECOMMENDATION,
    SEARCH_ENGINE,
    SOCIAL_MEDIA,
    LOCAL_AD,
    OTHER
}