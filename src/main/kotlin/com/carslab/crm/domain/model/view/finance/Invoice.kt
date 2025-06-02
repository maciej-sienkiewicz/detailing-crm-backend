package com.carslab.crm.domain.model.view.finance

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.clients.domain.model.ClientId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unikalny identyfikator faktury.
 */
data class InvoiceId(val value: String) {
    companion object {
        fun generate(): InvoiceId = InvoiceId(UUID.randomUUID().toString())
    }
}

/**
 * Status faktury.
 */
enum class InvoiceStatus {
    DRAFT,      // Szkic
    NOT_PAID,   // Nieopłacona
    PAID,       // Opłacona
    OVERDUE,    // Przeterminowana
    PARTIALLY_PAID, // Częściowo opłacona
    CANCELLED   // Anulowana
}

/**
 * Typ faktury.
 */
enum class InvoiceType {
    INCOME,     // Przychodowa
    EXPENSE     // Kosztowa
}

/**
 * Metoda płatności.
 */
enum class PaymentMethod {
    CASH,           // Gotówka
    BANK_TRANSFER,  // Przelew bankowy
    CARD,           // Karta płatnicza
    MOBILE_PAYMENT, // Płatność mobilna
    OTHER           // Inna
}

/**
 * Pozycja faktury.
 */
data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxRate: BigDecimal,
    val totalNet: BigDecimal,
    val totalGross: BigDecimal
)

/**
 * Załącznik do faktury.
 */
data class InvoiceAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val type: String,
    val storageId: String,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Model domenowy faktury.
 */
data class Invoice(
    val id: InvoiceId,
    val number: String,
    val title: String,
    val issuedDate: LocalDate,
    val dueDate: LocalDate,
    val sellerName: String,
    val sellerTaxId: String? = null,
    val sellerAddress: String? = null,
    val buyerName: String,
    val buyerTaxId: String? = null,
    val buyerAddress: String? = null,
    val clientId: ClientId? = null, // Opcjonalne powiązanie z klientem
    val status: InvoiceStatus,
    val type: InvoiceType,
    val paymentMethod: PaymentMethod,
    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,
    val currency: String,
    val notes: String? = null,
    val protocolId: String? = null,    // Opcjonalne powiązanie z protokołem
    val protocolNumber: String? = null,
    val items: List<InvoiceItem>,
    val attachment: InvoiceAttachment? = null,
    val audit: Audit
)