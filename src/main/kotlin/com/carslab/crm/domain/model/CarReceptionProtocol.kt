package com.carslab.crm.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Model domeny reprezentujący protokół przyjęcia pojazdu.
 */
data class CarReceptionProtocol(
    val id: ProtocolId,
    val vehicle: VehicleDetails,
    val client: Client,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val services: List<Service>,
    val notes: String? = null,
    val referralSource: ReferralSource? = null,
    val otherSourceDetails: String? = null,
    val documents: Documents = Documents(),
    val mediaItems: List<MediaItem> = emptyList(),
    val audit: AuditInfo
)

/**
 * Value object dla identyfikatora protokołu.
 */
data class ProtocolId(val value: String) {
    companion object {
        fun generate(): ProtocolId = ProtocolId("PROT-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}")
    }
}

/**
 * Informacje o pojeździe.
 */
data class VehicleDetails(
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Int? = null
)

/**
 * Informacje o kliencie.
 */
data class Client(
    val id: Long? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val taxId: String? = null
) {
    fun hasValidContactInfo(): Boolean = !(email.isNullOrBlank() && phone.isNullOrBlank())
}

/**
 * Okres świadczenia usługi.
 */
data class ServicePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate
)

/**
 * Informacje o usłudze.
 */
data class Service(
    val id: String,
    val name: String,
    val basePrice: Money,
    val discount: Discount? = null,
    val finalPrice: Money,
    val approvalStatus: ApprovalStatus,
)

/**
 * Prosty value object dla wartości pieniężnych.
 */
data class Money(val amount: Double, val currency: String = "PLN") {
    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract money with different currencies" }
        return Money(amount - other.amount, currency)
    }
}

/**
 * Informacje o rabacie.
 */
data class Discount(
    val type: DiscountType,
    val value: Double,
    val calculatedAmount: Money
)

/**
 * Typy rabatów.
 */
enum class DiscountType {
    PERCENTAGE,
    AMOUNT,
    FIXED_PRICE
}

/**
 * Status zatwierdzenia usługi.
 */
enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Status protokołu.
 */
enum class ProtocolStatus {
    SCHEDULED,
    PENDING_APPROVAL,
    IN_PROGRESS,
    READY_FOR_PICKUP,
    COMPLETED
}

/**
 * Źródło polecenia.
 */
enum class ReferralSource {
    REGULAR_CUSTOMER,
    RECOMMENDATION,
    SEARCH_ENGINE,
    SOCIAL_MEDIA,
    LOCAL_AD,
    OTHER
}

/**
 * Informacje o dokumentach.
 */
data class Documents(
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false
)

/**
 * Informacje o mediach (zdjęciach, itp.).
 */
data class MediaItem(
    val id: String,
    val type: MediaType,
    val name: String,
    val url: String? = null,
    val size: Long,
    val description: String? = null,
    val location: String? = null,
    val createdAt: LocalDateTime,
    val tags: List<String>
)

/**
 * Typ mediów.
 */
enum class MediaType {
    PHOTO,
    DOCUMENT,
    VIDEO
}

/**
 * Informacje audytowe.
 */
data class AuditInfo(
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val statusUpdatedAt: LocalDateTime,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val appointmentId: String? = null
)