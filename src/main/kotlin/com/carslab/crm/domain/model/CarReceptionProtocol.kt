package com.carslab.crm.domain.model

import java.time.LocalDateTime
import java.util.*

/**
 * Model domeny reprezentujący protokół przyjęcia pojazdu.
 */
data class CarReceptionProtocol(
    val id: ProtocolId,
    val title: String,
    val vehicle: VehicleDetails,
    val client: Client,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val protocolServices: List<ProtocolService>,
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
        // Ta metoda zostanie użyta tylko podczas migracji do nowo utworzonego protokołu
        // który jeszcze nie ma ID z bazy danych
        fun generate(): ProtocolId = ProtocolId(UUID.randomUUID().toString())
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
    val mileage: Long? = null
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
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * Informacje o usłudze.
 */
data class ProtocolService(
    val id: String,
    val name: String,
    val basePrice: Money,
    val discount: Discount? = null,
    val finalPrice: Money,
    val approvalStatus: ApprovalStatus,
    val note: String?,
    val quantity: Long,
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
enum class ProtocolStatus(val uiVale: String) {
    SCHEDULED("Zaplanowano"),
    PENDING_APPROVAL("Oczekuje na zatwierdzenie"),
    IN_PROGRESS("W realizacji"),
    READY_FOR_PICKUP("Gotowy do odbioru"),
    COMPLETED("Zakończony"),
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