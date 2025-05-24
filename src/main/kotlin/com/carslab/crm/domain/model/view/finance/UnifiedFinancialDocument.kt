package com.carslab.crm.domain.model.view.finance

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.Audit
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unikalny identyfikator dokumentu finansowego.
 */
data class UnifiedDocumentId(val value: String) {
    companion object {
        fun generate(): UnifiedDocumentId = UnifiedDocumentId(UUID.randomUUID().toString())
    }
}

/**
 * Pozycja dokumentu finansowego.
 */
data class DocumentItem(
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
 * Załącznik do dokumentu finansowego.
 */
data class DocumentAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val type: String,
    val storageId: String,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Model domenowy zunifikowanego dokumentu finansowego.
 * Zastępuje stary model faktur, obsługując różne typy dokumentów transakcyjnych.
 */
data class UnifiedFinancialDocument(
    val id: UnifiedDocumentId,
    val number: String,
    val type: DocumentType,
    val title: String,
    val description: String? = null,
    val issuedDate: LocalDate,
    val dueDate: LocalDate? = null,
    val sellerName: String,
    val sellerTaxId: String? = null,
    val sellerAddress: String? = null,
    val buyerName: String,
    val buyerTaxId: String? = null,
    val buyerAddress: String? = null,
    val status: DocumentStatus,
    val direction: TransactionDirection,
    val paymentMethod: PaymentMethod,
    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String,
    val notes: String? = null,
    val protocolId: String? = null,
    val protocolNumber: String? = null,
    val visitId: String? = null,
    val items: List<DocumentItem>,
    val attachment: DocumentAttachment? = null,
    val audit: Audit
) {
    /**
     * Sprawdza czy dokument jest w pełni zapłacony.
     */
    fun isPaid(): Boolean = paidAmount >= totalGross

    /**
     * Sprawdza czy dokument jest częściowo zapłacony.
     */
    fun isPartiallyPaid(): Boolean = paidAmount > BigDecimal.ZERO && paidAmount < totalGross

    /**
     * Sprawdza czy dokument jest przeterminowany.
     */
    fun isOverdue(): Boolean = dueDate?.let { LocalDate.now().isAfter(it) } ?: false

    /**
     * Oblicza pozostałą kwotę do zapłaty.
     */
    fun remainingAmount(): BigDecimal = (totalGross - paidAmount).max(BigDecimal.ZERO)

    /**
     * Sprawdza czy dokument ma załącznik.
     */
    fun hasAttachment(): Boolean = attachment != null
}